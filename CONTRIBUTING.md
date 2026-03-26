# Contributing to MoodleV2

Thank you for contributing! Please follow these guidelines to keep the codebase clean and the CI pipeline green.

---

## Getting Started

1. Clone the repository and install the Git hooks:
   ```bash
   git clone <repo-url>
   cd moodle-platform
   bash scripts/setup-hooks.sh
   ```

2. Set up the [environment variables](README.md#environment-variables).

3. Start the backend and frontend as described in the [README](README.md#getting-started).

---

## Branch Naming Convention

All branches **must** follow this pattern:

```
<type>/<short-description>
```

| Type | Purpose |
|------|---------|
| `feature` | New feature or enhancement |
| `bugfix` | Bug fix |
| `hotfix` | Urgent production fix |
| `chore` | Maintenance, dependencies, config |
| `refactor` | Code refactoring (no feature change) |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `release` | Release preparation |

**Examples:**
- `feature/quiz-timer`
- `bugfix/login-redirect-loop`
- `hotfix/jwt-expiry-crash`
- `chore/upgrade-spring-boot`
- `docs/update-readme`

> The `main` and `develop` branches are protected. Branch names are enforced by the pre-commit hook and CI.

---

## Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/). The `commit-msg` hook enforces this automatically.

```
<type>(<scope>): <description>
```

| Type | When to use |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation changes |
| `style` | Formatting, missing semicolons (no logic change) |
| `refactor` | Refactoring production code |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `build` | Build system or dependency changes |
| `ci` | CI/CD pipeline changes |
| `chore` | Other maintenance tasks |
| `revert` | Reverting a previous commit |

**Examples:**
```
feat(quiz): add countdown timer to quiz page
fix(auth): handle expired JWT gracefully
docs(readme): add environment variables table
refactor(course): extract enrollment logic to service
ci: add SpotBugs to GitHub Actions pipeline
```

---

## Code Quality Tools

### Backend (Java)

| Tool | Command | Purpose |
|------|---------|---------|
| **Spotless** | `./mvnw spotless:check` | Code formatting (Google Java Format, AOSP style) |
| **Spotless fix** | `./mvnw spotless:apply` | Auto-fix formatting issues |
| **SpotBugs** | `./mvnw compile spotbugs:check -DskipTests` | Static bug analysis |
| **Tests** | `./mvnw test` | Run unit tests |
| **Full verify** | `./mvnw verify` | Compile + format + bugs + tests |

### Frontend (Angular / TypeScript)

| Tool | Command | Purpose |
|------|---------|---------|
| **Build** | `npx ng build` | Compile and type-check |
| **Tests** | `npx ng test --watch=false --browsers=ChromeHeadless` | Unit tests |
| **Prettier** | Configured in `package.json` | Code formatting |

---

## Git Hooks

After running `bash scripts/setup-hooks.sh`, three hooks are active:

### `pre-commit`
- Validates branch name against naming convention
- Scans staged files for forbidden patterns (`System.out.print`, `console.log`, etc.)
- Runs **Spotless check** if Java files are staged
- Runs **Angular build** if TypeScript/HTML files are staged

### `commit-msg`
- Enforces **Conventional Commits** format on every commit message

### `pre-push`
- Runs **Spotless** + **SpotBugs** + **backend tests** if Java files changed
- Runs **frontend build** + **frontend tests** if frontend files changed

---

## Pull Request Workflow

1. Create a branch following the [naming convention](#branch-naming-convention).
2. Make your changes and commit using [Conventional Commits](#commit-message-convention).
3. Push your branch — the pre-push hook will run all checks.
4. Open a Pull Request against `develop` (or `main` for hotfixes).
5. CI will automatically run:
   - Branch name validation
   - Spotless formatting check
   - SpotBugs static analysis
   - Backend build & tests
   - Frontend lint, build & tests
6. Get a code review and merge.

---

## CI/CD Pipeline

### CI (on every push & PR to `main` / `develop`)
- **Backend Spotless** — formatting check
- **Backend SpotBugs** — static analysis
- **Backend Build & Test** — compile + unit tests
- **Frontend Lint & Typecheck** — production build
- **Frontend Tests** — Karma/Jasmine headless
- **Branch Name Gate** — convention check on PRs

### CD (on version tags `v*`)
- Builds backend JAR artifact
- Builds frontend production dist
- Creates a GitHub Release with artifacts
