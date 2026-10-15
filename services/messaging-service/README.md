# messaging-service

Chat between teachers and students: the REST API, live delivery over WebSocket (STOMP), and the conversation history.

- **Port:** 8082
- **Database:** `messaging` (Flyway migrations in `src/main/resources/db/migration`)
- **Owns:** chat messages

## API

Through the gateway (user token, CSRF checked there):

| Path | What |
|---|---|
| `/api/chat/**` | Contacts, conversations, sending, read state |
| `/ws/**` | WebSocket (STOMP); the gateway adds the user token to the handshake |

## Events (Kafka)

Consumes (idempotently; failing records end on `<topic>.DLT`):

| Topic | Reaction |
|---|---|
| `identity.user` | UserEmailChanged: history follows the new address; UserDeleted: history removed |
| `identity.session` | SessionsRevoked: closes that session's sockets |
| `assessment.attempt` | QuizAttemptChanged: locks or unlocks chat during a proctored quiz |

## Calls

| Service | For |
|---|---|
| identity-service | directory |
| academic-service | course membership (who may talk to whom) |
| assessment-service | open exam locks |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `DB_URL, DB_USERNAME, DB_PASSWORD` |  | Its own MySQL database |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `IDENTITY_URL` | http://localhost:8083 | identity-service: token endpoint, JWKS, directory |
| `INTERNAL_CLIENT_SECRET` |  | This service's secret at identity-service's token endpoint |
| `ACADEMIC_URL` | http://localhost:8081 | academic-service |
| `ASSESSMENT_URL` | http://localhost:8084 | assessment-service |
| `APP_CORS_ALLOWED_ORIGINS` | http://localhost:4200 | Origins allowed to open a WebSocket |
| `PORT` | 8082 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/messaging-service .
```

Integration tests start their own MySQL and Kafka with Testcontainers; a fake (MockWebServer) plays the services it calls.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs MySQL, Kafka, identity-service, academic-service and assessment-service.
