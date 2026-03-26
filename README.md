# MoodleV2 — Learning Management System

A full-stack **Learning Management System (LMS)** inspired by Moodle, built with a modern tech stack. The platform supports three user roles — **Student**, **Teacher**, and **Admin** — and delivers course management, quizzes, assignments, real-time chat, a calendar, grades, and more.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Backend](#backend-setup)
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
| **Backend** | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Spring WebSocket |
| **Frontend** | Angular 20, Angular Material, TypeScript, SCSS, STOMP.js / SockJS |
| **Mobile** | Apache Cordova (iOS build wrapping the Angular app) |
| **Database** | MySQL / MariaDB |
| **Migrations** | Flyway |
| **Auth** | JWT (jjwt 0.12), OAuth2 (Google, Facebook), TOTP-based 2FA |
| **Email** | Spring Mail (SMTP / Gmail) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Browser Extension** | Chrome Extension (Manifest V3) packaged alongside the frontend |

---

## Project Structure

```
moodle-platform/
├── backend/                  # Spring Boot REST API
│   ├── src/main/java/moodlev2/
│   │   ├── application/      # Application services (use cases)
│   │   ├── domain/           # Domain models & port interfaces
│   │   ├── infrastructure/   # JPA entities, repositories, adapters
│   │   ├── web/              # REST controllers & DTOs
│   │   └── common/           # Shared exceptions & utilities
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/     # Flyway SQL migrations
├── frontend/                 # Angular SPA
│   ├── src/app/
│   │   ├── core/             # Guards, interceptors, services, models
│   │   └── features/         # Feature modules (auth, courses, quiz, etc.)
│   ├── manifest.json         # Chrome Extension manifest (MV3)
│   └── package.json
└── moodle-mobile/            # Cordova wrapper for iOS
    ├── config.xml
    └── www/                  # Built Angular assets
```

---

## Features

### Authentication & Security
- Email / password registration and login
- **OAuth2** social login (Google, Facebook)
- **Two-Factor Authentication** (TOTP) setup and verification
- JWT-based stateless authentication with session tracking
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

- **Java 21** (JDK)
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **Node.js 20+** and **npm**
- **MySQL 8** or **MariaDB 10.6+**
- *(Optional)* Apache Cordova CLI for mobile builds

---

## Getting Started

### Backend Setup

1. **Set environment variables** (see [Environment Variables](#environment-variables)).

2. **Run the database migrations** automatically on startup via Flyway, or manually apply `V1__Init_Moodle_Schema.sql` and `V2__Seed_Initial_Users.sql`.

3. **Start the backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   The API will start on `http://localhost:8080`.

4. **Swagger UI** is available at `http://localhost:8080/swagger-ui.html`.

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
   The app will be served at `http://localhost:4200`.

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

The backend reads the following environment variables (configured in `application.properties`):

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC connection URL (e.g. `jdbc:mysql://localhost:3306/moodlev2`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Secret key for signing JWT tokens |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `FACEBOOK_CLIENT_ID` | Facebook OAuth2 client ID |
| `FACEBOOK_CLIENT_SECRET` | Facebook OAuth2 client secret |
| `MAIL_USERNAME` | SMTP email address (Gmail) |
| `MAIL_PASSWORD` | SMTP email app password |

---

## Database

- **Engine**: MySQL / MariaDB
- **ORM**: Spring Data JPA with Hibernate (validate mode)
- **Migrations**: Flyway (`V1` creates the full schema, `V2` seeds initial users)

### Core Tables

| Table | Purpose |
|-------|---------|
| `users` / `user_roles` | User accounts and role assignments |
| `classes` | Student class groups |
| `courses` / `course_classes` | Courses and class assignments |
| `enrollments` | Student-course enrollments |
| `course_modules` / `module_items` | Course content structure |
| `announcements` | Course announcements |
| `calendar_events` | Calendar events |
| `grades` | Student grades |
| `quizzes` / `quiz_questions` / `quiz_options` | Quiz definitions |
| `quiz_attempts` / `quiz_responses` | Quiz attempt tracking |
| `categories` / `tags` / `questions` / `question_bank_options` | Question bank |
| `assignment_submissions` | Assignment submissions & grading |
| `chat_messages` | Chat message history |
| `user_sessions` | Active session tracking |
| `password_reset_tokens` | Password reset tokens |

---

## API Overview

All endpoints are under `/api`. Authentication is required unless noted.

| Prefix | Description |
|--------|-------------|
| `/api/auth` | Login, register, forgot/reset password, 2FA, OAuth2 |
| `/api/users` | Profile, change password, sessions, teachers list |
| `/api/courses` | Dashboard, course CRUD, modules, resources, students |
| `/api/assignments` | Assignment details, student submissions, teacher grading |
| `/api/quizzes` | Create, start, submit, update, delete quizzes |
| `/api/question-bank` | Categories, questions CRUD |
| `/api/grades` | Student grades page |
| `/api/calendar` | Calendar events |
| `/api/chat` | Chat history (REST) + WebSocket messaging |
| `/api/admin` | Admin operations (courses, users, classes, gradebook) |

---

## Architecture

The backend follows a **layered / hexagonal architecture**:

```
web/          → REST controllers & DTOs (driving adapters)
application/  → Use-case services (application logic)
domain/       → Domain models, enums, port interfaces
infrastructure/ → JPA entities, repositories, adapters (driven adapters)
```

Key patterns:
- **Port & Adapter** interfaces for token service, password hashing, and user repository
- **Service-per-use-case** design (e.g. `GetCourseDetailsService`, `QuizEngineService`)
- **Flyway** for versioned database migrations
- **JWT + Spring Security** filter chain for stateless auth
- **WebSocket (STOMP/SockJS)** for real-time chat

---

## CI/CD Pipeline

GitHub Actions workflows are defined in `.github/workflows/`.

### CI (`ci.yml`) — runs on every push & PR to `main` / `develop`

| Job | What it does |
|-----|--------------|
| **Backend · Spotless Check** | Enforces Google Java Format (AOSP) |
| **Backend · SpotBugs** | Static bug analysis |
| **Backend · Build & Test** | Compiles and runs unit tests |
| **Frontend · Lint & Typecheck** | Production Angular build |
| **Frontend · Unit Tests** | Karma/Jasmine headless tests |
| **Branch Name Convention** | Rejects PRs from incorrectly named branches |

### CD (`cd.yml`) — runs on version tags (`v*`)

- Builds backend JAR and frontend dist
- Creates a GitHub Release with build artifacts

---

## Code Quality

| Tool | Layer | Command |
|------|-------|---------|
| **Spotless** | Backend | `./mvnw spotless:check` / `./mvnw spotless:apply` |
| **SpotBugs** | Backend | `./mvnw compile spotbugs:check -DskipTests` |
| **Prettier** | Frontend | Configured in `package.json` |
| **EditorConfig** | All | `.editorconfig` at project root |

### Git Hooks

Install with:
```bash
bash scripts/setup-hooks.sh
```

| Hook | Checks |
|------|--------|
| `pre-commit` | Branch naming, forbidden patterns, Spotless, Angular build |
| `commit-msg` | Conventional Commits format (`feat(scope): message`) |
| `pre-push` | Spotless, SpotBugs, backend tests, frontend build & tests |

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
