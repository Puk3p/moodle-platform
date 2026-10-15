# MoodleV2 — Learning Management System

A full-stack **Learning Management System (LMS)** inspired by Moodle. The platform supports three user roles — **Student**, **Teacher**, and **Admin** — and delivers course management, quizzes, assignments, real-time chat, a calendar, grades, and more.

The backend is a set of **microservices**: five Spring Boot applications, each with its own database, behind a Spring Cloud Gateway, talking over REST and **Kafka**. It started as a single Spring Boot application and was split one bounded context at a time, with the app working end to end after every step (see [Architecture](#architecture)).

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [The platform (Docker Compose)](#the-platform-docker-compose)
  - [Frontend](#frontend-setup)
  - [Mobile App](#mobile-app-setup)
- [Environment Variables](#environment-variables)
- [Database](#database)
- [API Overview](#api-overview)
- [Architecture](#architecture)
- [CI/CD Pipeline](#cicd-pipeline)
- [Code Quality](#code-quality)
- [Contributing](#contributing)
- [License](#license)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Services** | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Spring WebSocket |
| **Edge** | Spring Cloud Gateway (WebFlux) |
| **Messaging** | Apache Kafka (KRaft), Spring Kafka, Spring Modulith event publication registry (transactional outbox) |
| **Resilience** | Resilience4j (circuit breakers, retries), short client timeouts |
| **Frontend** | Angular 20, Angular Material, TypeScript, SCSS, STOMP.js / SockJS |
| **Mobile** | Apache Cordova (iOS build wrapping the Angular app) |
| **Database** | MySQL 8, one database per service |
| **Migrations** | Flyway |
| **Auth** | HttpOnly session cookie, RS256 internal tokens (OAuth2 client credentials, JWKS), OAuth2 login (Google, Facebook), TOTP-based 2FA |
| **Email** | Spring Mail (SMTP / Gmail) |
| **Testing** | JUnit 5, Mockito, Testcontainers (MySQL, Kafka), MockWebServer, end-to-end suite against the running stack |
| **Deployment** | Docker, Docker Compose |
| **Browser Extension** | Chrome Extension (Manifest V3) packaged alongside the frontend |

---

## Project Structure

```
moodle-platform/
├── pom.xml                       # Aggregator: ./mvnw verify builds and tests every service
├── services/
│   ├── identity-service/         # Accounts, classes, sign-in, sessions; issues internal tokens
│   ├── academic-service/         # Courses, modules, resources, assignments, membership
│   ├── assessment-service/       # Quizzes, attempts, proctoring, question bank
│   ├── progress-service/         # Grades and course calendars
│   └── messaging-service/        # Chat (REST + WebSocket)
├── edge/gateway/                 # The only public entry point: routing, session → token, CSRF, CORS
├── e2e/                          # End-to-end tests against the running stack (-Pe2e)
├── deploy/
│   ├── compose.yml               # The whole platform: MySQL, Kafka, the services, the gateway
│   ├── Dockerfile                # One image per service (--build-arg MODULE=...)
│   ├── .env.example              # Production settings
│   ├── mysql/init/               # One database and account per service
│   └── data-moves/               # One-time SQL to move an existing monolith's data
├── frontend/                     # Angular SPA (+ Chrome Extension manifest)
└── moodle-mobile/                # Cordova wrapper for iOS
```

Every service has the same layout: `web/` (controllers, DTOs), `application/` (use cases), `infrastructure/` (persistence, Kafka, security, clients for other services), `src/main/resources/db/migration/` (its own Flyway history).

---

## Features

### Authentication & Security
- Email / password registration and login
- **OAuth2** social login (Google, Facebook)
- **Two-Factor Authentication** (TOTP) setup and verification
- Server-side sessions in an HttpOnly, SameSite=Strict cookie, bound to the browser, with idle and absolute expiry
- Password reset via email token
- Role-based access control (`STUDENT`, `TEACHER`, `ADMIN`)
- Auth guard on all protected routes

### Course Management
- Create, edit, and preview courses (code, name, description, term, image)
- Organize content into **modules** with ordered **module items** (resources, assignments)
- Assign courses to student **classes**
- Enroll / manage students per course
- Upload and manage course resources (files up to 50 MB)
- Course announcements

### Assignments
- Teachers create assignments with configurable submission type (File, Text, or Both)
- Due dates and max grade configuration
- Students submit text responses and/or file uploads
- Teacher grading dashboard with per-submission feedback
- Assignment overview with submission statistics

### Quiz System
- Full quiz engine: create, edit, delete quizzes
- Multiple question types with configurable options
- Quiz settings: duration, max attempts, passing score, shuffle options, access password
- Availability windows (`available_from` / `available_to`)
- Manual or auto-generated quizzes from the **Question Bank**
- Student quiz-taking flow with timed attempts
- Automatic scoring and result review
- Teacher quiz results dashboard with per-attempt review

### Question Bank
- Centralized question repository with **categories** and **tags**
- CRUD operations on questions (with optional image uploads)
- Difficulty levels and usage tracking
- Reuse questions across multiple quizzes

### Grades
- Aggregated grades page for students
- Per-course grade items (score, max score, weight, type)
- Admin gradebook view

### Calendar
- Calendar events linked to courses
- Event types and descriptions

### Real-Time Chat
- Private messaging between users via **WebSocket** (STOMP over SockJS)
- Persistent chat history stored in the database

### User & Session Management
- User profile retrieval
- Change password
- View active sessions (device, IP)
- Revoke individual or all other sessions

### Admin Panel
- Manage users and students
- Manage classes
- Gradebook administration
- Course creation

### Chrome Extension
- Manifest V3 browser extension bundled with the frontend
- Enhances the Moodle experience with additional tools

### Mobile App
- **Apache Cordova** wrapper targeting iOS
- Serves the built Angular app as a native mobile application

---

## Prerequisites

- **Docker** with Docker Compose v2 (runs MySQL, Kafka and every service)
- **Java 21** (JDK), to build and test outside Docker; the Maven wrapper `./mvnw` is included
- **Node.js 20+** and **npm**, for the frontend
- *(Optional)* Apache Cordova CLI for mobile builds

---

## Getting Started

### The platform (Docker Compose)

1. **Start everything** (MySQL, Kafka, the five services and the gateway):
   ```bash
   docker compose -f deploy/compose.yml up -d --build --wait
   ```
   The API answers on `http://localhost:8080` (the gateway). Each service migrates and seeds its own database on first start, so the starter accounts (`student@test.com`, `teacher@test.com`, `admin@test.com`) and courses are there.

2. **Run the end-to-end tests** against it (optional):
   ```bash
   ./mvnw -f e2e/pom.xml verify -Pe2e
   ```

3. **Build and test every service** without Docker Compose (integration tests start their own MySQL and Kafka with Testcontainers, so Docker must be running):
   ```bash
   ./mvnw verify
   ```

Moving an existing single-database installation over: start the stack, then run the scripts in `deploy/data-moves/` (each one explains itself) before letting traffic in.

### Frontend Setup

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Start the development server**:
   ```bash
   npm start
   ```
   The app will be served at `http://localhost:4200` and talks to the gateway on `http://localhost:8080`.

### Mobile App Setup

1. **Install Cordova globally**:
   ```bash
   npm install -g cordova
   ```

2. **Build the Angular app** and copy the output to `moodle-mobile/www/`.

3. **Run on iOS**:
   ```bash
   cd moodle-mobile
   cordova platform add ios
   cordova run ios
   ```

---

## Environment Variables

`deploy/compose.yml` has working defaults for local development. For anything else copy `deploy/.env.example` to `deploy/.env` and fill it in:

| Variable | Description |
|----------|-------------|
| `MYSQL_ROOT_PASSWORD`, `*_DB_PASSWORD` | The MySQL root password and one password per service account |
| `JWT_SECRET` | identity-service's key for the short pre-2FA challenge token (≥ 32 bytes) |
| `INTERNAL_CLIENT_*_SECRET` | Each service's secret for identity-service's token endpoint (client credentials) |
| `INTERNAL_JWT_PRIVATE_KEY` / `INTERNAL_JWT_PUBLIC_KEY` | RS256 key pair for internal tokens; needed with more than one identity-service instance |
| `APP_SESSION_COOKIE_SECURE`, `SESSION_COOKIE_NAME` | `true` / `__Host-mdl_session` behind HTTPS |
| `TRUSTED_PROXIES`, `IDENTITY_TRUSTED_PROXIES` | Who may report client addresses (the reverse proxy, the gateway) |
| `PUBLIC_BASE_URL`, `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS` | Public URLs (OAuth redirects, reset links, CORS) |
| `GOOGLE_CLIENT_ID` / `_SECRET`, `FACEBOOK_CLIENT_ID` / `_SECRET` | OAuth2 sign-in |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP (password reset mail) |

---

## Database

One MySQL server, **one database and account per service**; an account can only reach its own database. Each service owns its Flyway history. Services keep other services' ids (users, courses) as plain columns, never foreign keys: what cascades used to do is done by reacting to events (an account deletion removes enrollments, submissions, attempts, grades and chat history in their own services).

| Database | Owner | Tables |
|----------|-------|--------|
| `identity` | identity-service | `users`, `user_roles`, `classes`, `user_sessions`, `password_reset_tokens`, `profile_pictures` |
| `academic` | academic-service | `courses`, `course_classes`, `enrollments`, `course_modules`, `module_items`, `announcements`, `assignment_submissions` |
| `assessment` | assessment-service | `quizzes`, `quiz_questions`, `quiz_options`, `quiz_attempts`, `quiz_responses`, `proctor_events`, question bank tables |
| `progress` | progress-service | `grades`, `calendar_events` |
| `messaging` | messaging-service | `chat_messages` |

Services that publish events also have `EVENT_PUBLICATION`, the transactional outbox.

---

## API Overview

All endpoints are under `/api` on the gateway; the frontend never sees which service answers. Authentication is required unless noted.

| Prefix | Service | Description |
|--------|---------|-------------|
| `/api/auth`, `/oauth2`, `/login/oauth2` | identity | Login, register, forgot/reset password, 2FA, OAuth2 |
| `/api/users` (except `/me`) | identity | Change password, sessions, profile picture, teachers list |
| `/api/admin/students`, `/api/admin/classes`, `/api/classes` | identity | Account and class administration |
| `/api/users/me` | academic | Profile with enrolled or taught courses |
| `/api/courses`, `/api/resources`, `/api/assignments`, `/api/teacher/dashboard` | academic | Dashboard, course CRUD, modules, resources, assignments, grading |
| `/api/quizzes`, `/api/teacher/quizzes`, `/api/question-bank` | assessment | Quizzes, attempts, results, question bank |
| `/api/grades`, `/api/admin/grades`, `/api/calendar` | progress | Grades, gradebook, calendar |
| `/api/chat`, `/ws` | messaging | Chat history (REST) and live messages (WebSocket) |

---

## Architecture

```
 Browser (Angular SPA)
   │  HttpOnly session cookie + X-XSRF-TOKEN header
   ▼
 gateway (Spring Cloud Gateway) ── CORS · CSRF check · session cookie → 60 s user token · routing
   │                    │ REST + RS256 bearer token
   ▼                    ▼
 identity-service    academic-service   assessment-service   progress-service   messaging-service
 (cookie, sessions,  (courses,          (quizzes,            (grades,           (chat,
  tokens, JWKS)       coursework)        question bank)       calendar)          WebSocket)
   │                    │                  │                    │                  │
 identity DB         academic DB        assessment DB        progress DB        messaging DB
   └──────────── Kafka: identity.user · identity.session · academic.course ·
                        academic.coursework · assessment.attempt ────────────────┘
```

**Bounded contexts.** Each service owns one area of the domain and its data. They ask each other synchronously only when they need an answer to serve a request (`/internal/**` REST APIs, which the gateway never routes), and otherwise react to each other's events.

**Authentication (phantom token).** The browser keeps an opaque HttpOnly session cookie, set and read only by identity-service. On every other route the gateway exchanges it at identity-service for a 60-second RS256 token carrying the user id, email, roles and class, caches it briefly, and forwards it as a bearer token; the cookie never leaves the gateway. Services validate tokens against identity-service's JWKS. Service-to-service calls use OAuth2 client-credentials tokens from the same issuer. Logging out or an admin change evicts the gateway's cache at once through `identity.session` / `identity.user` events.

**Events.** Changes other services must hear about are written to a transactional outbox (Spring Modulith's event publication registry) in the same transaction as the change, then published to Kafka as JSON with a logical `event-type` header, keyed by the id they must stay ordered by. Consumers are idempotent; a record that keeps failing goes to `<topic>.DLT` after retries with backoff.

| Topic | Events | Consumed by |
|-------|--------|-------------|
| `identity.user` | `UserEmailChanged`, `UserDeleted` | academic, assessment, progress, messaging, gateway |
| `identity.session` | `SessionsRevoked` | messaging (closes sockets), gateway (cache) |
| `academic.course` | `ModuleRemoved` | assessment (unfiles the module's quizzes) |
| `academic.coursework` | `AssignmentPublished` | progress (calendar deadline) |
| `assessment.attempt` | `QuizAttemptChanged` | messaging (locks chat during a proctored quiz) |

**Resilience.** Every client has short timeouts, one retry for reads and a circuit breaker per downstream service. Pages degrade instead of failing where they can (a dashboard without its calendar when progress-service is down); without identity-service, which every request needs, services answer 503 quickly.

**Inside each service** the original layered / hexagonal structure stays: `web/` → `application/` → ports → `infrastructure/` adapters, with service-per-use-case classes (e.g. `GetCourseDetailsService`, `QuizEngineService`).

---

## CI/CD Pipeline

GitHub Actions workflows are defined in `.github/workflows/`.

### CI (`ci.yml`) — runs on every push & PR to `main` / `develop`

| Job | What it does |
|-----|--------------|
| **Services · &lt;module&gt;** | Per service and the gateway: Spotless, unit tests, integration tests (Testcontainers), SpotBugs |
| **Platform · End-to-end** | Starts the whole stack with Docker Compose and runs the end-to-end suite through the gateway |
| **Frontend · Lint & Typecheck** | Production Angular build |
| **Frontend · Unit Tests** | Karma/Jasmine headless tests |
| **Branch Name Convention** | Rejects PRs from incorrectly named branches |

### CD (`cd.yml`) — runs on version tags (`v*`)

- Builds every service's JAR and the frontend dist
- Creates a GitHub Release with build artifacts

---

## Code Quality

| Tool | Layer | Command |
|------|-------|---------|
| **Spotless** | Services | `./mvnw -f services/<name>/pom.xml spotless:apply` (checked in `verify`) |
| **SpotBugs** | Services | runs in `./mvnw verify` |
| **Prettier** | Frontend | Configured in `package.json` |
| **EditorConfig** | All | `.editorconfig` at project root |

### Git Hooks

Install with:
```bash
bash scripts/setup-hooks.sh
```

| Hook | Checks |
|------|--------|
| `pre-commit` | Branch naming, forbidden patterns, Spotless (changed services), Angular build |
| `commit-msg` | Conventional Commits format (`feat(scope): message`) |
| `pre-push` | Spotless, unit tests and SpotBugs per changed service, frontend build & tests |

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide, including:
- Branch naming convention (`feature/`, `bugfix/`, `hotfix/`, etc.)
- Conventional Commits format
- Code quality tooling
- Pull request workflow

---

## License

This project is licensed under the [MIT License](LICENSE).
