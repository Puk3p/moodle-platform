# academic-service

Courses, their modules and resources, assignments and submissions, announcements, and who belongs to which course (enrolled directly, or through an assigned class). It also serves the profile page, which lists the courses someone takes or teaches.

- **Port:** 8081
- **Database:** `academic` (Flyway migrations in `src/main/resources/db/migration`)
- **Owns:** courses, course classes, enrollments, modules, module items, announcements, assignment submissions; uploaded files in `uploads/`

## API

Through the gateway (user token, CSRF checked there):

| Path | What |
|---|---|
| `/api/courses/**` | Dashboard, course pages, create and edit courses, students |
| `/api/resources/**, /api/assignments/**` | Resources and uploads, assignments, submissions, grading |
| `/api/announcements/**, /api/teacher/dashboard` | Announcements, teacher dashboard |
| `/api/users/me` | Profile with enrolled or taught courses |

Internal, for the gateway and other services only (never routed from outside; service token):

| Endpoint | What |
|---|---|
| `/internal/catalog/**` | Courses (by id, code, ids) and modules |
| `/internal/membership/**` | Students of a course, whether someone belongs to it, courses of a student, teacher or user |

## Events (Kafka)

Publishes, through a transactional outbox (`EVENT_PUBLICATION`):

| Topic | Events |
|---|---|
| `academic.course` | ModuleRemoved |
| `academic.coursework` | AssignmentPublished |

Consumes (idempotently; failing records end on `<topic>.DLT`):

| Topic | Reaction |
|---|---|
| `identity.user` | UserDeleted: removes their enrollments and submissions (with files), clears them as teacher |

## Calls

| Service | For |
|---|---|
| identity-service | directory (accounts, classes); without it requests fail fast with 503 |
| progress-service | course calendars; pages show no deadlines if it is down |
| assessment-service | quizzes on course pages; none shown if it is down |

## Configuration

Environment variables (defaults suit local development):

| Variable | Default | What |
|---|---|---|
| `DB_URL, DB_USERNAME, DB_PASSWORD` |  | Its own MySQL database |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `IDENTITY_URL` | http://localhost:8083 | identity-service: token endpoint, JWKS, directory |
| `INTERNAL_CLIENT_SECRET` |  | This service's secret at identity-service's token endpoint |
| `PROGRESS_URL` | http://localhost:8085 | progress-service |
| `ASSESSMENT_URL` | http://localhost:8084 | assessment-service |
| `SPRINGDOC_ENABLED` | false | Swagger UI (admins only) |
| `PORT` | 8081 |  |

## Build, test, run

From this folder:

```bash
# Formatting, unit tests, integration tests (needs Docker), SpotBugs
./mvnw verify
# The image deploy/compose.yml runs
docker build -t moodle/academic-service .
```

Integration tests start their own MySQL and Kafka with Testcontainers; a fake identity-service (MockWebServer) supplies keys, tokens and the directory.

To run it with everything else, from the repository root: `docker compose -f deploy/compose.yml up -d --build`. It needs MySQL, Kafka and identity-service (tokens and the directory).
