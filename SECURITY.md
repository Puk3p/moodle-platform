# Security & Production Hardening

This document summarises the security controls in place and the steps required before
deploying MoodleV2 for a real school (target scale: ~300 users).

## What was hardened in the code

| Area | Control |
|------|---------|
| **Authorization** | `/api/admin/**` now requires `ADMIN`; `/api/teacher/**` and `/api/question-bank/**` require `TEACHER`/`ADMIN`. Teacher-only actions (upload/delete resources, grade submissions, edit courses, create announcements, view rosters) are additionally guarded with method-level `@PreAuthorize`. Previously **any authenticated user could reach admin endpoints** (create courses, list/delete users). |
| **WebSocket auth** | The chat handshake now cryptographically verifies the JWT signature and expiry. Previously the token payload was base64-decoded **without signature verification**, letting anyone impersonate any user. |
| **Chat** | Message sender is bound to the authenticated principal (no spoofing); history is scoped to the caller (no reading others' private messages). |
| **CORS** | Wildcard `*` replaced with an environment-driven allow-list (`APP_CORS_ALLOWED_ORIGINS`). The duplicate wildcard MVC CORS mapping was removed. |
| **Rate limiting** | Per-client fixed-window limiter on `/api/auth/**` (login, register, password reset, 2FA) to blunt brute-force / credential-stuffing. Returns HTTP 429. |
| **User enumeration** | Login returns a single generic error and runs a constant-time dummy hash for unknown accounts; forgot-password no longer reveals whether an email exists. |
| **Password policy** | Minimum 8 chars incl. letters + numbers, enforced server-side on register, reset and change. |
| **File upload/download** | Extension allow-list (blocks HTML/executables → stored-XSS/RCE), size check, and path-traversal guards on store/load/delete. |
| **Secrets** | JWT secret is validated at startup (≥32 bytes, no placeholder). DB connector scripts containing a plaintext password were removed from the repo and git-ignored. |
| **Transport / headers** | HSTS, `X-Frame-Options: DENY`, and a Content-Security-Policy are sent. Error responses no longer leak messages/stack traces. |
| **Scalability** | HikariCP pool sizing, Tomcat thread/connection tuning, and `forward-headers-strategy` so the app runs correctly behind a reverse proxy / load balancer. |

## Required manual steps before go-live

1. **Rotate the leaked database credential.** The file
   `backend/src/main/resources/connectors/connect_mariadb_dutu.sh` previously contained a
   plaintext password (`dutu2002`) and an internal DB host. It has been removed, but the
   credential must be **rotated on the database server** — deleting the file does not
   invalidate an exposed password. It also remains in git history; consider history rewriting
   (e.g. `git filter-repo`) if the repository is or was public.
2. **Do not load the seed migration (`V2__Seed_Initial_Users.sql`) in production.** It creates
   `admin@test.com` / `teacher@test.com` / `student@test.com` with a shared, known bcrypt hash.
   Use a clean production database and create the first admin account through a controlled path.
3. **Set strong secrets** for `JWT_SECRET`, DB, OAuth and mail via environment variables
   (see `backend/.env.example`). Never commit `.env`.
4. **Terminate TLS** (HTTPS) at a reverse proxy (nginx/Caddy/Traefik) and point
   `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS`, and `OAUTH_BASE_URL` at the real HTTPS
   domains. Update the front-end WebSocket URL to `wss://`.
5. **Restrict Swagger UI / API docs** (or disable in production) if not needed by end users.

## Scaling to ~300 users

A single backend instance with the tuned Hikari pool (20 connections) and Tomcat threads
(200) comfortably serves a few hundred concurrent users — a dedicated load balancer is **not
required** for this scale. Put the app behind one reverse proxy for TLS and static-asset
caching. If you ever scale to multiple instances, note two stateful pieces that must move to a
shared store first: the in-memory rate limiter (→ Redis/Bucket4j) and the STOMP simple broker
(→ an external broker such as RabbitMQ).
