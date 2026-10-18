# Moving production to the services

A one-time cutover, run by hand on the VPS. Afterwards every push to `main` deploys by itself.

|  | Before | After |
|---|---|---|
| API on `127.0.0.1:8091` | one jar, `systemd moodle-backend.service` | the gateway container |
| Code | `/opt/moodle-platform/app.jar` | images `ghcr.io/<owner>/moodle/<service>:<commit>` |
| Database | `moodle` on the VPS's MySQL/MariaDB | one database per service in a MySQL 8 container |
| Secrets | SSM `/config/app/` → `/opt/moodle-platform/.env` | SSM `/config/platform/` → `/opt/moodle-platform/deploy/.env` |
| nginx | `/api`, `/ws`, `/oauth2`, `/login/oauth2` → `127.0.0.1:8091` | unchanged |

Because the gateway takes over the old port on loopback, nginx needs no change. Rolling back is
stopping the gateway and starting the old service again (step 7). Downtime: from step 5 to step 6,
mostly the database copy.

## 1. Check the server

```bash
free -h                                   # 8 GB; the stack uses about 4
df -h /var/lib/docker /opt                # room for ~3 GB of images and a copy of the database
systemctl cat moodle-backend              # WorkingDirectory: uploads/ and profile-pictures/ are in it
grep -E '^DB_(URL|USERNAME)=' /opt/moodle-platform/.env   # the old database, its name and user
mysql --version                           # MySQL or MariaDB, and which version
grep -rn 8091 /etc/nginx/                 # /api, /ws (with Upgrade headers), /oauth2, /login/oauth2
```

## 2. Install Docker

Docker Engine with the Compose plugin, from docs.docker.com/engine/install (Ubuntu or Debian). Then
`docker compose version` should print v2.

## 3. Secrets in SSM

One SecureString parameter per variable under `/config/platform/`; the parameter's last path segment
is the variable name, as under `/config/app/` today:

```bash
aws ssm put-parameter --region eu-north-1 --type SecureString \
  --name /config/platform/IDENTITY_DB_PASSWORD --value "$(openssl rand -base64 24)"
```

| Variable | Value |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `IDENTITY_DB_PASSWORD`, `ACADEMIC_DB_PASSWORD`, `ASSESSMENT_DB_PASSWORD`, `PROGRESS_DB_PASSWORD`, `MESSAGING_DB_PASSWORD` | new, `openssl rand -base64 24` each |
| `INTERNAL_CLIENT_GATEWAY_SECRET`, `INTERNAL_CLIENT_ACADEMIC_SECRET`, `INTERNAL_CLIENT_ASSESSMENT_SECRET`, `INTERNAL_CLIENT_PROGRESS_SECRET`, `INTERNAL_CLIENT_MESSAGING_SECRET` | new, `openssl rand -hex 32` each |
| `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FACEBOOK_CLIENT_ID`, `FACEBOOK_CLIENT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD` | the same values as under `/config/app/` |
| `APP_SESSION_COOKIE_SECURE` | `true` |
| `SESSION_COOKIE_NAME` | `__Host-mdl_session` (the cookie browsers hold today, so nobody is signed out) |
| `PUBLIC_BASE_URL`, `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS` | `https://moodle.puk3p.online` |
| `TRUSTED_PROXIES` | `127\.0\.0\.1\|172\.(1[6-9]\|2[0-9]\|3[01])\..*` (nginx reaches the gateway through Docker's bridge) |
| `IDENTITY_TRUSTED_PROXIES` | `10\.\d+\.\d+\.\d+\|172\.(1[6-9]\|2[0-9]\|3[01])\.\d+\.\d+\|192\.168\.\d+\.\d+\|127\.\d+\.\d+\.\d+` |
| `GATEWAY_HOST_PORT` | `8091` |

Without the two `*_TRUSTED_PROXIES` the local-development default applies, which believes any
client's `X-Forwarded-For` and so lets anyone sidestep the per-address sign-in rate limit.

## 4. Build the images

GitHub → Actions → **VPS Auto-Deploy** → **Run workflow** on `main`. It builds and pushes the six
images, tagged with the commit (note its SHA), and redeploys the frontend, which is harmless.
The images are private: on the server, `docker login ghcr.io` with a GitHub token that has
`read:packages`.

## 5. Prepare the stack (the old backend keeps serving)

From a checkout of `main` on your machine:

```bash
ssh root@VPS 'mkdir -p /opt/moodle-platform/deploy/mysql/init /opt/moodle-platform/deploy/data-moves'
scp deploy/compose.yml root@VPS:/opt/moodle-platform/deploy/
scp deploy/mysql/init/01-databases.sh root@VPS:/opt/moodle-platform/deploy/mysql/init/
scp deploy/data-moves/*.sql root@VPS:/opt/moodle-platform/deploy/data-moves/
```

On the server, write `.env` the way the deploy does, then pull and start the database and Kafka:

```bash
cd /opt/moodle-platform/deploy
umask 077
aws ssm get-parameters-by-path --region eu-north-1 --path /config/platform/ --with-decryption \
  --query "Parameters[*].[Name,Value]" --output text \
  | awk -F'\t' '{n=split($1,a,"/"); print a[n]"="$2}' > .env
echo "IMAGE_PREFIX=ghcr.io/<owner, lower case>/moodle" >> .env
echo "IMAGE_TAG=<the SHA from step 4>" >> .env

docker compose pull
docker compose up -d --no-build --wait mysql db-init kafka
```

The new MySQL listens on `127.0.0.1:3307` only, next to the existing one on 3306.

## 6. Cutover

```bash
cd /opt/moodle-platform/deploy
systemctl stop moodle-backend                       # downtime starts
```

Copy the old database into the new MySQL, as `moodle` (the data-move scripts read from there). The
dump is also your backup of the moment before the cutover:

```bash
mysqldump --single-transaction --no-tablespaces -u <old user> -p <old database> > /root/moodle-before-cutover.sql
# MariaDB 11 only, if the import below complains about a collation:
#   sed -i 's/utf8mb4_uca1400_ai_ci/utf8mb4_general_ci/g' /root/moodle-before-cutover.sql
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE moodle"'
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" moodle' < /root/moodle-before-cutover.sql
```

Start the services without the gateway (each creates its database), then copy the data in this order.
Each script prints counts to compare; the two sides must match:

```bash
docker compose up -d --no-build --wait identity-service academic-service assessment-service progress-service messaging-service
for move in accounts-to-identity courses-to-academic quizzes-to-assessment grades-and-calendar-to-progress chat-to-messaging; do
  echo "== $move"
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < data-moves/2026-10-move-$move.sql
done
```

Copy the files (the services run as uid 10001, so they must own them). With the old WorkingDirectory
from step 1 as `$OLD`:

```bash
OLD=/opt/moodle-platform
for vol in academic-uploads assessment-uploads; do
  docker run --rm -v "$OLD/uploads:/from:ro" -v "moodle_$vol:/to" alpine sh -c 'cp -a /from/. /to/ && chown -R 10001:10001 /to'
done
docker run --rm -v "$OLD/profile-pictures:/from:ro" -v moodle_identity-profile-pictures:/to alpine sh -c 'cp -a /from/. /to/ && chown -R 10001:10001 /to'
```

Then let traffic in:

```bash
docker compose up -d --no-build --wait gateway       # takes 127.0.0.1:8091; downtime ends
curl -s -o /dev/null -w '%{http_code}\n' https://moodle.puk3p.online/api/courses   # 401
systemctl disable moodle-backend                     # keep app.jar, .env and the old database
```

Sign in, open a course, a quiz, the grades and the chat. Browsers that were signed in stay signed in
(their sessions were copied). Last, in GitHub → Settings → Secrets and variables → Actions →
Variables, add `BACKEND_DEPLOY` = `compose`: from then on every push to `main` builds the images,
pulls them on the server and restarts what changed.

## 7. Rollback

The old database and jar are untouched, so going back is:

```bash
cd /opt/moodle-platform/deploy && docker compose stop gateway
systemctl enable --now moodle-backend
```

and removing the `BACKEND_DEPLOY` variable. Whatever was written through the new platform since the
cutover stays only in its databases.

## 8. Later, once all is well

- Back up the new databases: `docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup.sql`.
  identity's database holds the key that signs internal tokens, next to password hashes and 2FA
  secrets: keep its backups as safe as the key itself.
- Drop the `moodle` copy inside the new MySQL, retire the old database server if nothing else uses
  it, and remove the old service, jar and `/config/app/` parameters.
