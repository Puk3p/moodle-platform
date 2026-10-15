# identity-service

Accounts, classes, sign-in (password, 2FA, Google and Facebook) and browser sessions. It is the only service that reads the session cookie, and it issues the tokens every other service trusts: client-credentials tokens for services, and the short user tokens the gateway swaps the cookie for.

- **Port:** 8083
- **Database:** `identity` (Flyway migrations in `src/main/resources/db/migration`)
- **Owns:** users, roles, classes, sessions, password reset tokens, profile pictures

## API

Through the gateway (user token, CSRF checked there):

| Path | What |
|---|---|
| `/api/auth/**` | Register, login, logout, current session, forgot/reset password, 2FA |
| `/oauth2/**, /login/oauth2/**` | Google and Facebook sign-in |
| `/api/users/** (not /me)` | Change password, sessions, profile picture, teachers list |
| `/api/admin/students/**, /api/admin/classes/**, /api/classes/**` | Account and class administration |

The gateway passes these through untouched: this service sets and reads the session and XSRF cookies itself.

Internal, for the gateway and other services only (never routed from outside; service token):

| Endpoint | What |
|---|---|
| `POST /internal/oauth2/token` | Client-credentials grant (HTTP Basic), for the gateway and the services |
| `GET /internal/.well-known/jwks.json` | Public keys for checking its tokens |
| `POST /internal/sessions/resolve` | The gateway's session cookie → 60 s user token exchange |
| `/internal/directory/**` | Users (by id, email, class) and classes; `/users/{id}/account` adds 2FA status |

## Events (Kafka)

Publishes, through a transactional outbox (`EVENT_PUBLICATION`):

| Topic | Events |
|---|---|
| `identity.user` | UserEmailChanged, UserDeleted |
| `identity.session` | SessionsRevoked |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `DB_URL, DB_USERNAME, DB_PASSWORD` |  | Its own MySQL database |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `JWT_SECRET` |  | Key for the short pre-2FA challenge token (≥ 32 bytes) |
| `INTERNAL_CLIENT_{GATEWAY,ACADEMIC,ASSESSMENT,PROGRESS,MESSAGING}_SECRET` |  | Who may get tokens; a blank secret disables that client |
| `INTERNAL_JWT_PRIVATE_KEY, INTERNAL_JWT_PUBLIC_KEY` | generated per start | RS256 signing key pair (PEM); set it for more than one instance |
| `APP_SESSION_COOKIE_SECURE` | true | `false` only for plain-HTTP local development |
| `SERVER_TOMCAT_REMOTEIP_INTERNALPROXIES` | private networks | Proxies trusted for X-Forwarded-For (rate limits and session records use the client address) |
| `APP_FRONTEND_URL, OAUTH_BASE_URL` | localhost | Redirects after OAuth, links in reset mail |
| `GOOGLE_CLIENT_ID/SECRET, FACEBOOK_CLIENT_ID/SECRET` |  | OAuth2 sign-in |
| `MAIL_USERNAME, MAIL_PASSWORD` |  | SMTP for password reset mail |
| `PROFILE_PICTURES_DIR` | profile-pictures | Where pictures are stored |
| `PORT` | 8083 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/identity-service .
```

Integration tests start their own MySQL and Kafka with Testcontainers.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs only MySQL and Kafka.
