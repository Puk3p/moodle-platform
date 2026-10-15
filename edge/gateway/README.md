# gateway

The platform's only public entry point (Spring Cloud Gateway). It routes each path to the service that owns it, answers CORS, checks CSRF (double submit) and, for every service but identity-service, swaps the browser's session cookie for a short-lived user token, so services only ever see tokens.

- **Port:** 8080

## API

Routes:

| Path | Goes to |
|---|---|
| `/api/auth/**, /oauth2/**, /login/oauth2/**, /api/users/** (not /me), /api/admin/students/**, /api/admin/classes/**, /api/classes/**` | identity-service (cookies passed through) |
| `/api/quizzes/**, /api/teacher/quizzes/**, /api/question-bank/**` | assessment-service |
| `/api/grades/**, /api/admin/grades/**, /api/calendar/**` | progress-service |
| `/api/chat/**, /ws/**` | messaging-service |
| `/api/users/me and everything else` | academic-service |
| `/internal/**` | never: 404 |

## Events (Kafka)

Consumes (idempotently; failing records end on `<topic>.DLT`):

| Topic | Reaction |
|---|---|
| `identity.session, identity.user` | Drop cached tokens of revoked sessions and changed accounts at once (a consumer group per instance) |

## Calls

| Service | For |
|---|---|
| identity-service | session exchange (cached 20 s per session and browser), with its own client-credentials token |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `IDENTITY_URL, ACADEMIC_URL, ASSESSMENT_URL, PROGRESS_URL, MESSAGING_URL` | localhost:808x | Where the services are |
| `INTERNAL_CLIENT_SECRET` |  | The gateway's secret at identity-service's token endpoint |
| `SESSION_COOKIE_NAME` | __Host-mdl_session | `mdl_session` when identity-service runs without Secure cookies |
| `TRUSTED_PROXIES` | private networks | Who may report client addresses in X-Forwarded-For (the reverse proxy) |
| `APP_CORS_ALLOWED_ORIGINS` | http://localhost:4200 | The frontend origins |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `PORT` | 8080 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/gateway .
```

Integration tests start Kafka with Testcontainers and stand-ins (MockWebServer) for identity-service, academic-service and messaging-service.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs Kafka and the services it routes to.
