# assessment-service

Quizzes (authoring, attempts, timing, scoring, results), proctoring reports and the question bank.

- **Port:** 8084
- **Database:** `assessment` (Flyway migrations in `src/main/resources/db/migration`)
- **Owns:** quizzes, questions and options, attempts and responses, extra attempts, proctor events, question bank (categories, tags, questions); question images in `uploads/`

## API

Through the gateway (user token, CSRF checked there):

| Path | What |
|---|---|
| `/api/quizzes/**` | Quiz authoring, taking an attempt, results, proctoring |
| `/api/teacher/quizzes/**` | A teacher's quizzes |
| `/api/question-bank/**` | Categories and questions |

Internal, for the gateway and other services only (never routed from outside; service token):

| Endpoint | What |
|---|---|
| `GET /internal/quizzes/courses/{id}` | A course's published quizzes (academic-service's course page) |
| `GET /internal/exam-locks/users/{id}` | Open attempts that block chat (messaging-service) |

## Events (Kafka)

Publishes, through a transactional outbox (`EVENT_PUBLICATION`):

| Topic | Events |
|---|---|
| `assessment.attempt` | QuizAttemptChanged (started or finished; messaging locks chat) |

Consumes (idempotently; failing records end on `<topic>.DLT`):

| Topic | Reaction |
|---|---|
| `identity.user` | UserDeleted: removes the person's attempts |
| `academic.course` | ModuleRemoved: its quizzes stay, unfiled |

## Calls

| Service | For |
|---|---|
| identity-service | directory |
| academic-service | course catalog and membership |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `DB_URL, DB_USERNAME, DB_PASSWORD` |  | Its own MySQL database |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `IDENTITY_URL` | http://localhost:8083 | identity-service: token endpoint, JWKS, directory |
| `INTERNAL_CLIENT_SECRET` |  | This service's secret at identity-service's token endpoint |
| `ACADEMIC_URL` | http://localhost:8081 | academic-service |
| `UPLOADS_DIR` | uploads | Question images |
| `PORT` | 8084 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/assessment-service .
```

Integration tests start their own MySQL and Kafka with Testcontainers; a fake (MockWebServer) plays identity-service and academic-service.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs MySQL, Kafka, identity-service and academic-service.
