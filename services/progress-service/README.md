# progress-service

Grades and course calendars. Assignment deadlines arrive as events when teachers publish them.

- **Port:** 8085
- **Database:** `progress` (Flyway migrations in `src/main/resources/db/migration`)
- **Owns:** grades, calendar events

## API

Through the gateway (user token, CSRF checked there):

| Path | What |
|---|---|
| `/api/grades/**` | A student's grades |
| `/api/admin/grades/**` | Gradebook |
| `/api/calendar/**` | Calendar |

Internal, for the gateway and other services only (never routed from outside; service token):

| Endpoint | What |
|---|---|
| `POST /internal/calendar/courses` | Calendars of several courses (dashboard) |
| `GET /internal/calendar/courses/{id}/first` | A course's first entries (course preview) |

## Events (Kafka)

Consumes (idempotently; failing records end on `<topic>.DLT`):

| Topic | Reaction |
|---|---|
| `academic.coursework` | AssignmentPublished: adds or updates the deadline |
| `identity.user` | UserDeleted: removes the person's grades |

## Calls

| Service | For |
|---|---|
| identity-service | directory |
| academic-service | courses and membership |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `DB_URL, DB_USERNAME, DB_PASSWORD` |  | Its own MySQL database |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `IDENTITY_URL` | http://localhost:8083 | identity-service: token endpoint, JWKS, directory |
| `INTERNAL_CLIENT_SECRET` |  | This service's secret at identity-service's token endpoint |
| `ACADEMIC_URL` | http://localhost:8081 | academic-service |
| `PORT` | 8085 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/progress-service .
```

Integration tests start their own MySQL and Kafka with Testcontainers; a fake (MockWebServer) plays identity-service and academic-service.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs MySQL, Kafka, identity-service and academic-service.
