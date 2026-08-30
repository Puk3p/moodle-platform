# 🔒 Security Audit Report — Moodle Platform

**Audit Date:** 2026-08-30  
**Scope:** Backend (Spring Boot / Java) · Frontend (Angular) · Mobile (Cordova) · Browser Extension  
**Total Vulnerabilities Found: 55**

| Severity | Count |
|----------|-------|
| 🔴 CRITICAL | 9 |
| 🟠 HIGH | 18 |
| 🟡 MEDIUM | 18 |
| 🟢 LOW | 7 |
| ℹ️ INFO | 3 |

> [!CAUTION]
> This report contains details of real security vulnerabilities. Do not share publicly. Remediate CRITICAL and HIGH issues before any production deployment.

---

## Table of Contents

- [CRITICAL Vulnerabilities](#critical-vulnerabilities)
- [HIGH Vulnerabilities](#high-vulnerabilities)
- [MEDIUM Vulnerabilities](#medium-vulnerabilities)
- [LOW Vulnerabilities](#low-vulnerabilities)
- [INFO / Notes](#info--notes)
- [Summary Table](#summary-table)
- [Remediation Priority](#remediation-priority)

---

## CRITICAL Vulnerabilities

---

### [BE-C1] Quiz Password Stored in Plaintext

- **Severity:** 🔴 CRITICAL
- **Category:** Insecure Credential Storage
- **Layer:** Backend
- **Files:**
  - [`QuizEntity.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/domain/quiz/QuizEntity.java) — `access_password` column
  - [`QuizManagementService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/quiz/QuizManagementService.java) — L51
  - [`QuizEngineService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/quiz/QuizEngineService.java) — L50–57
- **Description:** Quiz access passwords are stored as raw plaintext in the `access_password` database column. Anyone with database read access (DBA, backup file leak, SQL injection) can read all quiz passwords directly.
- **Fix:** Hash quiz passwords with BCrypt before persistence. Compare using `passwordEncoder.matches(rawInput, storedHash)`. Never store any password in plaintext.

---

### [BE-C2] Unauthenticated File Download — `/uploads/**` Publicly Accessible

- **Severity:** 🔴 CRITICAL
- **Category:** Broken Access Control / Sensitive Data Exposure
- **Layer:** Backend
- **File:** [`SecurityConfig.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/SecurityConfig.java) — L71–72
- **Evidence:**
  ```java
  auth.requestMatchers("/uploads/**").permitAll()
  ```
- **Description:** The entire `/uploads/` directory (which contains assignment submissions, question images, and teacher materials) is accessible to **anyone on the internet without any authentication**. An attacker can enumerate or directly access any uploaded file by guessing or enumerating filenames.
- **Fix:** Remove `permitAll()` for `/uploads/**`. Serve files exclusively through the authenticated `/api/resources/download/{fileName}` endpoint and remove the static resource mapping for `uploads/`.

---

### [BE-C3] JWT Revocation Uses Only Last 15 Characters of Token

- **Severity:** 🔴 CRITICAL
- **Category:** Broken Authentication / Insecure Session Management
- **Layer:** Backend
- **Files:**
  - [`LoginService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/auth/LoginService.java) — L144–149
  - [`ManageSessionsService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/auth/ManageSessionsService.java) — L18, L39
- **Description:** The session revocation mechanism stores only the last 15 characters of the JWT signature as a `tokenSignature` identifier. This is cryptographically weak. More critically, a "revoked" token is still accepted by any service that validates the JWT signature without checking the revocation table — the revocation doesn't actually work unless every single endpoint checks this table.
- **Fix:** Implement a proper token blocklist using the JWT's `jti` (JWT ID) claim in Redis or a DB table. Use SHA-256 of the full token as the identifier. Every protected endpoint must check this blocklist before accepting a token.

---

### [FE-C1] JWT Token Stored in `localStorage` — Accessible to Any JavaScript

- **Severity:** 🔴 CRITICAL
- **Category:** Insecure Credential Storage / XSS Token Theft
- **Layer:** Frontend
- **Files:**
  - [`auth.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/auth.service.ts) — L84, 88, 92
  - [`web-socket.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/web-socket.service.ts) — L24
  - [`take-quiz.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/quiz/take-quiz/take-quiz.ts) — L385, 413, 418
- **Evidence:**
  ```typescript
  private saveToken(token: string): void {
    localStorage.setItem('token', token);  // accessible to ANY script on this origin
  }
  getToken(): string | null {
    return localStorage.getItem('token');
  }
  ```
- **Description:** The JWT bearer token is persisted in `localStorage`. Any XSS payload — including from a compromised third-party CDN loaded without SRI (see FE-C3) — can call `localStorage.getItem('token')` and exfiltrate the token for full account takeover. The WebSocket service also reads this token and embeds it in a URL query parameter visible in server logs.
- **Fix:** Store the JWT in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie — invisible to JavaScript entirely. If cookies are impractical short-term, use `sessionStorage` at minimum. Remove the token from WebSocket URLs (authenticate via STOMP `connectHeaders` instead).

---

### [FE-C2] OAuth Callback JWT Delivered via URL Query Parameter

- **Severity:** 🔴 CRITICAL
- **Category:** Authentication — OAuth Token Leakage
- **Layer:** Frontend
- **Files:**
  - [`app.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/app.ts) — L51–61
  - [`login.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/auth/login/login.ts) — L67–81
  - [`auth.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/auth.service.ts) — L163–175
- **Evidence:**
  ```typescript
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');  // JWT in URL — logged everywhere
  if (token) {
    this.authService.handleOAuthCallback(token);
  ```
- **Description:** The OAuth flow delivers the full JWT as `?token=...` in the redirect URL. This leaks the token into: server access logs, proxy logs, browser history, and Referer headers sent to the three third-party CDNs loaded on the page (FontAwesome, Google Fonts, cdnjs). `window.history.replaceState` removes it from the address bar but only after the Referer leaks have already occurred.
- **Fix:** Use OAuth Authorization Code Flow with PKCE. Deliver the authorization code (not the token) in the URL; exchange it server-side and set an HttpOnly cookie. As an interim measure, deliver the token in the URL **fragment** (`#token=...`) — fragments are not sent to servers and don't appear in Referer headers.

---

### [FE-C3] No Content Security Policy — XSS Amplification & CDN Supply Chain Risk

- **Severity:** 🔴 CRITICAL
- **Category:** Missing Security Headers / XSS Amplification
- **Layer:** Frontend / Mobile
- **Files:**
  - [`index.html`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/index.html) — entire file
  - [`moodle-mobile/www/index.html`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/moodle-mobile/www/index.html) — entire file
- **Evidence:**
  ```html
  <!-- No <meta http-equiv="Content-Security-Policy"> present -->
  <script src="https://kit.fontawesome.com/d1053bafa2.js" crossorigin="anonymous"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
  <!-- No integrity= SRI hash on any external resource -->
  ```
- **Description:** Neither the web app nor the Cordova app has a CSP. Scripts are loaded from 3 separate third-party origins without Subresource Integrity (SRI) hashes. A CDN compromise directly compromises every user session. Without a CSP, any injected script executes without restriction, stealing tokens from `localStorage` (see FE-C1).
- **Fix:**
  1. Add a strict CSP header via nginx: `default-src 'self'; script-src 'self'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; connect-src 'self' wss:; object-src 'none';`
  2. Add `integrity="sha384-..."` SRI hashes to all external `<script>` and `<link>` tags, or self-host all assets.
  3. Remove `(window as any).global = window` from `main.ts`.

---

### [MOB-C1] Cordova iOS — `NSAllowsArbitraryLoads = true` (TLS Completely Disabled)

- **Severity:** 🔴 CRITICAL
- **Category:** Insecure Transport (Mobile — iOS)
- **Layer:** Mobile
- **File:** [`config.xml`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/moodle-mobile/config.xml) — L25–30
- **Evidence:**
  ```xml
  <edit-config target="NSAppTransportSecurity" file="*-Info.plist" mode="merge">
      <dict>
          <key>NSAllowsArbitraryLoads</key>
          <true/>
      </dict>
  </edit-config>
  ```
- **Description:** iOS App Transport Security is completely disabled. The Cordova WebView can make plaintext HTTP connections to **any host**, transmitting JWT tokens, user data, and all API traffic in cleartext — fully visible to a network-level attacker (WiFi sniffing, MitM).
- **Fix:** Remove `NSAllowsArbitraryLoads`. Enforce HTTPS on all production endpoints. If an exception is genuinely needed for a specific domain, use `NSExceptionDomains` scoped to that domain only.

---

### [MOB-C2] Cordova Android — `android:usesCleartextTraffic="true"` Enabled

- **Severity:** 🔴 CRITICAL
- **Category:** Insecure Transport (Mobile — Android)
- **Layer:** Mobile
- **File:** [`config.xml`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/moodle-mobile/config.xml) — L35–37
- **Evidence:**
  ```xml
  <application android:usesCleartextTraffic="true" />
  ```
- **Description:** This overrides Android's default HTTPS-only policy (API level 28+), allowing the app to communicate over unencrypted HTTP and exposing all traffic to network-level attackers.
- **Fix:** Remove this flag. All API endpoints must be served over HTTPS. For local dev, use a Network Security Configuration file to allow cleartext only for `localhost` in debug builds.

---

### [EXT-C1] Browser Extension — `<all_urls>` Permission on Every Website

- **Severity:** 🔴 CRITICAL
- **Category:** Browser Extension — Overprivileged Permissions
- **Layer:** Browser Extension
- **File:** [`manifest.json`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/manifest.json) — L9, 17–22
- **Evidence:**
  ```json
  "host_permissions": ["<all_urls>"],
  "content_scripts": [{ "matches": ["<all_urls>"], "js": ["content.js"] }]
  ```
- **Description:** The extension injects `content.js` into **every single web page** the user visits (banking, email, social media, anything) and has `host_permissions` to read and modify any page. This is the maximum possible privilege for a browser extension. A Moodle companion extension has zero legitimate need for access to `https://gmail.com` or `https://bankofamerica.com`.
- **Fix:** Restrict both `host_permissions` and the content script `matches` to only the Moodle instance domain: `"*://your-moodle-domain.com/*"`.

---

## HIGH Vulnerabilities

---

### [BE-H1] JWT Token Leaked in OAuth Redirect URL (Backend Side)

- **Severity:** 🟠 HIGH
- **Category:** Authentication — Token Leakage
- **Layer:** Backend
- **File:** [`OAuth2LoginSuccessHandler.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/OAuth2LoginSuccessHandler.java) — L68
- **Evidence:**
  ```java
  response.sendRedirect(frontendUrl + "/login?token=" + token);
  ```
- **Description:** The full JWT is appended to the OAuth redirect URL. This exposes the token in server logs, browser history, and Referer headers. This is the backend root cause of FE-C2.
- **Fix:** Use a short-lived (60-second TTL) opaque one-time code stored in Redis. Redirect to `/login?code=<otp>`. The frontend exchanges the OTP for a JWT via a POST. Or, set an HttpOnly cookie directly in the OAuth success handler.

---

### [BE-H2] JWT Passed as WebSocket URL Query Parameter

- **Severity:** 🟠 HIGH
- **Category:** Sensitive Data Exposure / Token Leakage
- **Layer:** Backend
- **File:** [`WebSocketConfig.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/WebSocketConfig.java) — L57–65
- **Evidence:**
  ```java
  if (query == null || !query.contains("access_token=")) { ... }
  ```
- **Description:** The backend WebSocket handshake handler reads the JWT from the URL query parameter `?access_token=`. Query parameters appear in all server and proxy logs.
- **Fix:** Authenticate WebSocket connections via the STOMP CONNECT frame's `Authorization` header. Remove query-parameter auth support entirely.

---

### [BE-H3] WebSocket CORS Wildcard — `setAllowedOriginPatterns("*")`

- **Severity:** 🟠 HIGH
- **Category:** CORS Misconfiguration
- **Layer:** Backend
- **File:** [`WebSocketConfig.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/WebSocketConfig.java) — L32
- **Evidence:**
  ```java
  .setAllowedOriginPatterns("*")
  ```
- **Description:** The WebSocket endpoint accepts connections from any origin, completely bypassing the fine-grained CORS policy in `CorsConfig.java`. Any malicious website can initiate a WebSocket connection to the server on behalf of a logged-in user (Cross-Site WebSocket Hijacking — CSWSH).
- **Fix:** Replace with `.setAllowedOrigins(allowedOrigins)` using the same env-driven allowlist from `CorsConfig.java`.

---

### [BE-H4] Mass Assignment — Raw JPA Entity Accepted in Admin Course Creation

- **Severity:** 🟠 HIGH
- **Category:** Mass Assignment / Data Tampering
- **Layer:** Backend
- **File:** [`AdminController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/admin/AdminController.java) — L17–20
- **Evidence:**
  ```java
  public CourseEntity createCourse(@RequestBody CourseEntity course) {
      return courseRepository.save(course);
  }
  ```
- **Description:** The admin course creation endpoint accepts the raw JPA entity from the request body. An attacker can supply arbitrary fields present on `CourseEntity` (e.g., `id`, `createdAt`, relationship IDs), leading to data tampering and database integrity violations.
- **Fix:** Use a dedicated `CreateCourseDto` and map it to the entity manually. Never bind HTTP request bodies directly to JPA entities.

---

### [BE-H5] Missing Role Authorization on Quiz Create/Update/Delete

- **Severity:** 🟠 HIGH
- **Category:** Broken Authorization — Privilege Escalation
- **Layer:** Backend
- **File:** [`QuizController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/quiz/QuizController.java) — L23–52
- **Description:** `POST /api/quizzes/create`, `DELETE /api/quizzes/{id}`, and `PUT /api/quizzes/{id}` are protected only by authentication (any role). Any logged-in **student** can create, modify, or delete quizzes.
- **Fix:** Add `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")` to all three endpoints.

---

### [BE-H6] Missing Ownership Check on Quiz Attempt Review and Score Editing (IDOR)

- **Severity:** 🟠 HIGH
- **Category:** Insecure Direct Object Reference (IDOR)
- **Layer:** Backend
- **File:** [`TeacherQuizController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/quiz/TeacherQuizController.java) — L38–48
- **Description:** `GET /api/teacher/quizzes/attempts/{attemptId}/review` and `PATCH /api/teacher/quizzes/attempts/{attemptId}/questions/{questionId}/score` accept any `attemptId`. A teacher can view or modify grades for attempts on any quiz — not just their own courses' quizzes.
- **Fix:** Before returning data or saving scores, verify that the quiz associated with the attempt belongs to a course taught by the requesting teacher.

---

### [BE-H7] Missing Ownership Check on Assignment Submission View (IDOR)

- **Severity:** 🟠 HIGH
- **Category:** IDOR — Unauthorized Data Access
- **Layer:** Backend
- **Files:**
  - [`AssignmentController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/resource/AssignmentController.java) — L37–40
  - [`AssignmentService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/resource/AssignmentService.java) — L202–224
- **Description:** `GET /api/assignments/submissions/{id}` returns any submission by ID with no check that the requesting teacher owns the course the assignment belongs to. Any teacher can access any student's submission.
- **Fix:** Verify that the submission's assignment belongs to a course taught by the authenticated teacher before returning the data.

---

### [BE-H8] IDOR on Resource Visibility Toggle and Delete

- **Severity:** 🟠 HIGH
- **Category:** IDOR — Unauthorized Resource Modification
- **Layer:** Backend
- **Files:**
  - [`ResourceController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/resource/ResourceController.java) — L80–90
  - [`ResourceService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/resource/ResourceService.java) — L133–155
- **Description:** `PATCH /api/resources/{id}/visibility` and `DELETE /api/resources/{id}` check only for `TEACHER` or `ADMIN` role, without verifying that the resource belongs to a course the teacher actually teaches. Any teacher can delete or hide resources from any other teacher's course.
- **Fix:** Add ownership verification: confirm the resource's module → course → teacher matches the authenticated user before mutating it.

---

### [BE-H9] Rate Limiter Trusts Unvalidated `X-Forwarded-For` Header

- **Severity:** 🟠 HIGH
- **Category:** Rate Limiting Bypass
- **Layer:** Backend
- **File:** [`RateLimitFilter.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/RateLimitFilter.java) — L83–88
- **Evidence:**
  ```java
  String forwarded = request.getHeader("X-Forwarded-For");
  ```
- **Description:** The IP key for rate limiting is read directly from `X-Forwarded-For` without validation or trust-chain verification. An attacker can trivially bypass all rate limits by spoofing this header.
- **Fix:** Configure Spring's `ForwardedHeaderFilter` + `RemoteIpFilter` with a trusted proxy allowlist. Or use `request.getRemoteAddr()` unconditionally if the proxy is configured to strip the header before setting its own.

---

### [FE-H1] No Role Guard on Teacher/Admin Routes — Any Student Can Access Them

- **Severity:** 🟠 HIGH
- **Category:** Broken Access Control
- **Layer:** Frontend
- **File:** [`app.routes.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/app.routes.ts) — L89–163
- **Evidence:**
  ```typescript
  { path: 'gradebook',      canActivate: [authGuard], ... },  // ADMIN only — no role check
  { path: 'students',       canActivate: [authGuard], ... },  // ADMIN only — no role check
  { path: 'manage-courses', canActivate: [authGuard], ... },  // TEACHER only — no role check
  { path: 'grade-assignment/:submissionId', canActivate: [authGuard], ... } // TEACHER only
  ```
- **Description:** All teacher-only and admin-only routes are protected only by `authGuard`, which checks `localStorage.getItem('token') !== null`. Any logged-in student can navigate directly to any teacher or admin page. The `*ngIf` role checks in templates are purely cosmetic — they don't block direct URL access.
- **Fix:** Implement a `roleGuard` / `teacherGuard` / `adminGuard` and apply them to the appropriate routes. Note: client-side guards are a UX convenience only; the backend must also enforce role authorization.

---

### [FE-H2] JWT Token in WebSocket URL Query Parameter

- **Severity:** 🟠 HIGH
- **Category:** Sensitive Data Exposure — Token Leakage
- **Layer:** Frontend
- **File:** [`web-socket.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/web-socket.service.ts) — L34–36
- **Evidence:**
  ```typescript
  const wsUrl = token
    ? `${wsOrigin}/ws/websocket?access_token=${encodeURIComponent(token)}`
    : `${wsOrigin}/ws/websocket`;
  ```
- **Description:** The JWT is embedded in the WebSocket URL and visible in server/proxy logs. The STOMP `connectHeaders` already includes `Authorization: Bearer <token>` — the URL token is redundant and dangerous.
- **Fix:** Remove the query parameter from the URL. Authenticate exclusively via STOMP `connectHeaders`.

---

### [FE-H3] Open Redirect via Server-Controlled URL in `window.open()`

- **Severity:** 🟠 HIGH
- **Category:** Open Redirect / Phishing
- **Layer:** Frontend
- **Files:**
  - [`course-page.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/courses/course-page/course-page.ts) — L137–139, 145–146
  - [`course-resources.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/teacher/course-resources/course-resources.ts) — L157–159, 163–164
  - [`resources-page.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/resources/resources-page/resources-page.ts) — L88–91
- **Evidence:**
  ```typescript
  if (typeStr.includes('link') || url.startsWith('http')) {
    window.open(url, '_blank');  // url comes directly from server API response
  }
  ```
- **Description:** Server-returned `url` values are passed directly to `window.open()` without domain validation. A compromised or malicious backend entry can redirect users to arbitrary phishing sites.
- **Fix:** Validate server-returned URLs against an allowlist of trusted domains. Use `window.open(sanitizedUrl, '_blank', 'noopener,noreferrer')`.

---

### [FE-H4] STOMP WebSocket Debug Logging Active in Production

- **Severity:** 🟠 HIGH
- **Category:** Information Disclosure
- **Layer:** Frontend
- **File:** [`web-socket.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/web-socket.service.ts) — L44
- **Evidence:**
  ```typescript
  debug: (str) => console.log('STOMP: ' + str),  // logs Authorization header in prod
  ```
- **Description:** All STOMP frames — including authentication headers — are logged to the browser console with no environment check. Anyone with DevTools open can harvest session tokens.
- **Fix:** `debug: environment.production ? () => {} : (str) => console.log('STOMP:', str)`

---

### [FE-H5] `isLoggedIn()` Does Not Check JWT Expiry

- **Severity:** 🟠 HIGH
- **Category:** Broken Session Management
- **Layer:** Frontend
- **File:** [`auth.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/auth.service.ts) — L95–101
- **Evidence:**
  ```typescript
  isLoggedIn(): boolean {
    return !!this.getToken();  // no expiry check — expired tokens pass
  }
  ```
- **Description:** A user with an expired JWT will still pass `authGuard` and appear authenticated client-side.
- **Fix:** Decode the JWT on every `isLoggedIn()` call and compare the `exp` claim to `Date.now() / 1000`. Auto-call `logout()` and redirect to `/login` on expiry.

---

### [FE-H6] Password Reset Token Passed via URL Query Parameter

- **Severity:** 🟠 HIGH
- **Category:** Authentication — Password Reset Token Leakage
- **Layer:** Frontend
- **File:** [`reset-password.component.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/auth/reset-password/reset-password.component.ts) — L29
- **Evidence:**
  ```typescript
  this.token = this.route.snapshot.queryParams['token'];
  ```
- **Description:** Password reset tokens are in the URL query string, making them visible in server logs, browser history, and Referer headers.
- **Fix:** Deliver reset tokens via the URL **fragment** (`#token=...`). Ensure tokens are single-use and expire in ≤30 minutes.

---

### [FE-H7] Path Traversal Risk — Unsanitized Filename Used in Download URL

- **Severity:** 🟠 HIGH
- **Category:** Path Traversal / IDOR
- **Layer:** Frontend
- **Files:**
  - [`resources.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/resources.service.ts) — L40–44
  - [`resources-page.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/resources/resources-page/resources-page.ts) — L94
- **Evidence:**
  ```typescript
  const filename = file.url.split('/').pop();  // unsanitized server-returned value
  this.resourcesService.downloadFile(filename).subscribe(...)
  ```
- **Description:** A malicious server-returned `url` value (e.g., `../../etc/passwd`) would produce a crafted `filename` sent to the download endpoint.
- **Fix:** Reject any `filename` containing `..`, `/`, or `\`. Backend must also validate strictly and use opaque resource IDs.

---

### [FE-H8] Exam Answer Text Persisted to `localStorage`

- **Severity:** 🟠 HIGH
- **Category:** Insecure Storage of Sensitive Data
- **Layer:** Frontend
- **File:** [`take-quiz.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/quiz/take-quiz/take-quiz.ts) — L399–414
- **Evidence:**
  ```typescript
  localStorage.setItem('quiz_state_' + this.attemptId, JSON.stringify({
    answers: this.questions.map(q => ({ qId: q.id, optId: q.selectedOptionId, text: q.textAnswer, ... })),
  }));
  ```
- **Description:** Full exam answers including free-text responses are saved persistently to `localStorage`. In shared-device scenarios, a subsequent user can read prior answers.
- **Fix:** Use `sessionStorage` (cleared on tab close) instead. Add explicit cleanup in `ngOnDestroy`.

---

### [MOB-H1] Cordova — `<access origin="*" />` Allows Requests to Any Internet Host

- **Severity:** 🟠 HIGH
- **Category:** Mobile — Overprivileged Network Access
- **Layer:** Mobile
- **File:** [`config.xml`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/moodle-mobile/config.xml) — L13
- **Evidence:**
  ```xml
  <access origin="*" />
  ```
- **Description:** The Cordova WebView whitelist allows the app to make requests to any internet host. If any content is exploitable (XSS, open redirect), an attacker can exfiltrate data to arbitrary external servers.
- **Fix:** Restrict to the production backend: `<access origin="https://your-moodle-domain.com" />`.

---

## MEDIUM Vulnerabilities

---

### [BE-M1] Weak Password Policy — No Special Character Requirement
- **File:** [`PasswordPolicy.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/common/PasswordPolicy.java)
- Min 8 chars, one letter, one digit — no special character, no uppercase requirement. Insufficient for an LMS with student PII.
- **Fix:** Require uppercase + lowercase + digit + special character, or min length 12.

### [BE-M2] Missing `@Valid` on Forgot/Reset Password Request Bodies
- **File:** [`AuthController.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/web/auth/AuthController.java) — L50–57
- `ResetPasswordRequest` has no `@NotBlank` annotations; null/empty values pass through.
- **Fix:** Add `@Valid` to both request bodies. Add `@NotBlank` + `@Size(min=8)` to all fields.

### [BE-M3] 2FA TOTP Uses SHA-1
- **File:** [`TwoFactorService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/auth/TwoFactorService.java) — L50
- TOTP configured with `HashingAlgorithm.SHA1`. SHA-256 is preferred.
- **Fix:** Use `HashingAlgorithm.SHA256` where authenticator app support allows.

### [BE-M4] 2FA Has No Per-Account Brute-Force Lockout
- `/api/auth/login/verify-2fa` has global IP rate limiting but no per-user TOTP failure counter. 6-digit TOTP (1,000,000 combinations) brutable via distributed IPs.
- **Fix:** Add per-user 2FA failure counter; lock after 5 consecutive failures for 30 minutes.

### [BE-M5] No CSRF Protection for WebSocket STOMP Messages
- **File:** [`SecurityConfig.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/infrastructure/security/SecurityConfig.java) — L33
- CSRF disabled globally. Combined with wildcard WebSocket CORS (BE-H3), CSWSH is possible.
- **Fix:** Implement STOMP channel-level origin validation using Spring Security 6 message security.

### [BE-M6] Spring Boot DevTools Included in Runtime Build
- **File:** [`pom.xml`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/pom.xml) — L65–70
- `spring-boot-devtools` at `<scope>runtime</scope>` enables remote restart/reloading in production.
- **Fix:** Move to `<scope>test</scope>` or gate behind a Maven profile excluded from production CI.

### [BE-M7] Swagger UI / OpenAPI Docs Not Restricted in Production
- `springdoc-openapi` exposes `/swagger-ui.html` and `/v3/api-docs` — not restricted to admin role.
- **Fix:** Add `.requestMatchers("/swagger-ui/**", "/v3/**").hasRole("ADMIN")` or disable entirely in prod profile.

### [BE-M8] Password Reset Token Not Invalidated on Password Change
- Existing valid reset tokens remain usable after a successful password change.
- **Fix:** In `ChangePasswordService.changePassword()`, delete all `PasswordResetTokenEntity` records for the user.

### [BE-M9] Chat Message Content Not Sanitized — Stored XSS Risk
- **File:** [`ChatService.java`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/java/moodlev2/application/chat/ChatService.java) — L43–44
- `chatMessage.getContent()` stored and relayed without sanitization. Stored XSS if rendered as HTML.
- **Fix:** Sanitize with OWASP Java HTML Sanitizer before storing or broadcasting.

### [BE-M10] `flyway.validate-on-migrate=false`
- **File:** [`application.properties`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/backend/src/main/resources/application.properties) — L21
- Schema drift between migration scripts and actual DB won't be caught at startup.
- **Fix:** Set `spring.flyway.validate-on-migrate=true`.

### [FE-M1] No CSRF Token on State-Mutating API Requests
- **File:** [`auth.interceptor.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/interceptors/auth.interceptor.ts)
- If the app migrates to HttpOnly cookie auth (recommended), all POST/PUT/DELETE/PATCH requests will be CSRF-vulnerable.
- **Fix:** Prepare now: implement Angular's `HttpClientXsrfModule`.

### [FE-M2] `window.open()` Without `noopener noreferrer` — Reverse Tabnapping
- **Files:** `course-page.ts` L138, L145, L226-228; `course-resources.ts` L158, L164; `resources-page.ts` L89
- Without `noopener`, newly opened pages can redirect the parent Moodle tab.
- **Fix:** Always use `window.open(url, '_blank', 'noopener,noreferrer')`.

### [FE-M3] `prompt()` Used to Collect Quiz Passwords
- **Files:** [`take-quiz.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/quiz/take-quiz/take-quiz.ts) L140; `course-page.ts` L199
- Native `prompt()` displays input as **plaintext** (not masked). Security anti-pattern for passwords.
- **Fix:** Use an Angular Material dialog with `<input type="password">`.

### [FE-M4] Development Environment Uses Plain HTTP
- **File:** [`environment.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/environments/environment.ts) — L7–8 — `http://localhost:8080`
- If accidentally deployed, all JWT communication is in cleartext.
- **Fix:** Add pipeline guards preventing production deployment of non-production builds.

### [FE-M5] Browser Extension Content Script Has a Syntax Error
- **File:** [`content.js`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/content.js) — L1 — `console.log\`('content file');\`` (tagged template literal, not a call)
- The content script is a broken placeholder that runs on every website (via `<all_urls>`).
- **Fix:** Fix syntax. Implement code review for any future additions given this script's privileged access.

### [FE-M6] No File Type or Size Validation on Assignment Submissions
- **File:** [`assignment-submit.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/student/assignment-submit/assignment-submit.ts) — L113–121
- No client-side validation of file type or size before upload.
- **Fix:** Validate `file.type` against allowlist (PDF, DOCX, ZIP) and enforce max size (e.g., 50 MB).

### [FE-M7] Admin Destructive Actions Have No Role Guard
- **Files:** [`admin-students.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/admin/admin-students/admin-students.ts) L178–199; [`admin.service.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/core/services/admin.service.ts) L21–27
- `disable-2fa` and `deleteStudent` operations are only gated by `authGuard`. Any logged-in user can call these endpoints.
- **Fix:** Add `adminGuard` to the `/students` and `/gradebook` routes.

### [FE-M8] Sensitive Profile Data Logged to Browser Console
- **Files:** [`settings-page.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/settings/settings-page/settings-page.ts) L139, 168–169; `auth.service.ts` L159
- Full user profile objects logged in production code: `console.log('Save profile (TODO)', this.profile)`.
- **Fix:** Remove all `console.log/error` from production. Gate behind `!environment.production`.

---

## LOW Vulnerabilities

---

### [BE-L1] `enabled`/`active` Field Name Mismatch Between Domain and Entity
- `User.java` uses `enabled`; `UserEntity.java` uses `active`. If mapping breaks, disabled accounts could log in.
- **Fix:** Align naming across domain model and JPA entity.

### [FE-L1] Font Awesome Kit Loaded with Personal Kit ID and No SRI
- **File:** `index.html` L9 — `https://kit.fontawesome.com/d1053bafa2.js` — no `integrity=` attribute.
- Kit ID exposed publicly; CDN compromise executes arbitrary code on all users.
- **Fix:** Lock kit to production domain in FA settings. Add SRI hash or self-host assets.

### [FE-L2] `alert()` / `confirm()` Used for Security-Sensitive Actions
- Native browser dialogs used for password reset confirmations, quiz scores, and destructive admin actions.
- **Fix:** Replace with Angular Material `MatDialog` everywhere.

### [FE-L3] Hardcoded Term Filter — Business Logic Bypass Risk
- **File:** [`resources-page.ts`](file:///Users/georgelupu/Desktop/Proiecte%20Personale/moodle-platform/frontend/src/app/features/resources/resources-page/resources-page.ts) L17 — `termOptions = ['Fall 2024', 'Spring 2024', 'Fall 2023']`
- Users can manipulate the term filter via DevTools to request resources from terms they're not enrolled in.
- **Fix:** Fetch available terms from the server based on user enrollments. Enforce access server-side.

### [FE-L4] `(window as any).global = window` — Global Namespace Pollution
- **File:** `main.ts` L1 — expands prototype pollution attack surface.
- **Fix:** Identify which dependency requires this shim and update it or use a proper polyfill.

### [MOB-L1] Cordova `config.xml` Retains Default Apache Author Details
- **File:** `config.xml` L7 — `dev@cordova.apache.org`. Update with actual developer contact.

### [FE-L5] `authService` Exposed as `public` — Full Service with `getToken()` in Template Context
- **File:** `app.ts` L27 — public service injection increases XSS token theft impact surface.
- **Fix:** Make services `private`. Expose only minimal getters needed by the template.

---

## INFO / Notes

### [BE-I1] Full Role Set Returned in `AuthResponse`
- Returning roles explicitly in auth response encourages frontend role-checking that can be spoofed. Backend must always be the authoritative enforcer.

### [FE-I1] No Client-Side Login Attempt Throttle
- No CAPTCHA or cooldown on the login form. Add client-side exponential back-off after failed attempts. Rely primarily on server-side rate limiting (see BE-H9).

### [FE-I2] No Auto-Logout or 401 Interceptor on JWT Expiry
- An idle user remains on protected pages with a stale session indefinitely. Add a 401 HTTP interceptor calling `authService.logout()` and a background timer based on the JWT `exp` claim.

---

## Summary Table

| ID | Sev | Layer | File | Description |
|----|-----|-------|------|-------------|
| BE-C1 | 🔴 | Backend | QuizEntity.java | Quiz passwords in plaintext |
| BE-C2 | 🔴 | Backend | SecurityConfig.java | `/uploads/**` publicly accessible |
| BE-C3 | 🔴 | Backend | LoginService.java | JWT revocation uses last-15-chars |
| FE-C1 | 🔴 | Frontend | auth.service.ts | JWT in localStorage |
| FE-C2 | 🔴 | Frontend | app.ts / login.ts | OAuth token in URL query param |
| FE-C3 | 🔴 | Frontend | index.html | No Content-Security-Policy + no SRI |
| MOB-C1 | 🔴 | Mobile | config.xml | iOS NSAllowsArbitraryLoads=true |
| MOB-C2 | 🔴 | Mobile | config.xml | Android cleartext traffic enabled |
| EXT-C1 | 🔴 | Extension | manifest.json | `<all_urls>` host permission |
| BE-H1 | 🟠 | Backend | OAuth2LoginSuccessHandler.java | JWT in OAuth redirect URL |
| BE-H2 | 🟠 | Backend | WebSocketConfig.java | JWT in WebSocket URL query param |
| BE-H3 | 🟠 | Backend | WebSocketConfig.java | WebSocket allows all origins |
| BE-H4 | 🟠 | Backend | AdminController.java | Raw JPA entity in request body |
| BE-H5 | 🟠 | Backend | QuizController.java | Students can create/delete quizzes |
| BE-H6 | 🟠 | Backend | TeacherQuizController.java | No quiz attempt ownership check (IDOR) |
| BE-H7 | 🟠 | Backend | AssignmentController.java | No submission ownership check (IDOR) |
| BE-H8 | 🟠 | Backend | ResourceController.java | No resource ownership check (IDOR) |
| BE-H9 | 🟠 | Backend | RateLimitFilter.java | X-Forwarded-For spoofing bypass |
| FE-H1 | 🟠 | Frontend | app.routes.ts | No role guard on teacher/admin routes |
| FE-H2 | 🟠 | Frontend | web-socket.service.ts | JWT in WebSocket URL |
| FE-H3 | 🟠 | Frontend | course-page.ts | Server URL in window.open() |
| FE-H4 | 🟠 | Frontend | web-socket.service.ts | STOMP debug logging in production |
| FE-H5 | 🟠 | Frontend | auth.service.ts | No JWT expiry check in isLoggedIn() |
| FE-H6 | 🟠 | Frontend | reset-password.ts | Reset token in URL query param |
| FE-H7 | 🟠 | Frontend | resources.service.ts | Unsanitized filename in download URL |
| FE-H8 | 🟠 | Frontend | take-quiz.ts | Exam answers in localStorage |
| MOB-H1 | 🟠 | Mobile | config.xml | `<access origin="*" />` |
| BE-M1 | 🟡 | Backend | PasswordPolicy.java | Weak password policy |
| BE-M2 | 🟡 | Backend | AuthController.java | Missing @Valid on reset password |
| BE-M3 | 🟡 | Backend | TwoFactorService.java | 2FA uses SHA-1 |
| BE-M4 | 🟡 | Backend | 2FA endpoints | No per-account 2FA lockout |
| BE-M5 | 🟡 | Backend | SecurityConfig.java | No STOMP CSRF protection |
| BE-M6 | 🟡 | Backend | pom.xml | DevTools in runtime scope |
| BE-M7 | 🟡 | Backend | SecurityConfig.java | Swagger UI unrestricted |
| BE-M8 | 🟡 | Backend | ChangePasswordService | Reset tokens not revoked on pw change |
| BE-M9 | 🟡 | Backend | ChatService.java | Chat content not sanitized (stored XSS) |
| BE-M10 | 🟡 | Backend | application.properties | flyway.validate-on-migrate=false |
| FE-M1 | 🟡 | Frontend | auth.interceptor.ts | No CSRF token header |
| FE-M2 | 🟡 | Frontend | course-page.ts | window.open without noopener |
| FE-M3 | 🟡 | Frontend | take-quiz.ts | prompt() for password input |
| FE-M4 | 🟡 | Frontend | environment.ts | HTTP in dev environment |
| FE-M5 | 🟡 | Extension | content.js | Broken content script syntax |
| FE-M6 | 🟡 | Frontend | assignment-submit.ts | No file type/size validation |
| FE-M7 | 🟡 | Frontend | admin-students.ts | No role check for admin destructive actions |
| FE-M8 | 🟡 | Frontend | settings-page.ts | Profile data logged to console |
| BE-L1 | 🟢 | Backend | User.java | enabled/active field name mismatch |
| FE-L1 | 🟢 | Frontend | index.html | FontAwesome kit without SRI |
| FE-L2 | 🟢 | Frontend | Multiple | alert()/confirm() for security actions |
| FE-L3 | 🟢 | Frontend | resources-page.ts | Hardcoded term filter |
| FE-L4 | 🟢 | Frontend | main.ts | global=window shim |
| MOB-L1 | 🟢 | Mobile | config.xml | Default Cordova author details |
| FE-L5 | 🟢 | Frontend | app.ts | Public service injection |
| BE-I1 | ℹ️ | Backend | AuthResponse | Full roles in auth response |
| FE-I1 | ℹ️ | Frontend | login.ts | No client-side login throttle |
| FE-I2 | ℹ️ | Frontend | auth.service.ts | No auto-logout on token expiry |

---

## Remediation Priority

### 🚨 Immediate — Before Any Production Deployment

1. **[BE-C2]** Remove public access to `/uploads/**`
2. **[FE-C1]** Move JWT from `localStorage` to HttpOnly cookie
3. **[BE-C1]** Hash quiz passwords with BCrypt
4. **[MOB-C1] + [MOB-C2]** Remove iOS ATS override and Android cleartext flag
5. **[EXT-C1]** Restrict browser extension to Moodle domain only
6. **[FE-C3]** Add Content Security Policy headers + SRI hashes on all external resources
7. **[BE-H5]** Add `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")` to quiz endpoints
8. **[BE-H4]** Replace raw JPA entity in admin endpoint with a proper DTO

### ⚡ Short-Term — Within Current Sprint

9. **[FE-C2] + [BE-H1]** Fix OAuth callback — use one-time code, not token in URL
10. **[BE-H3]** Restrict WebSocket CORS to known origins list
11. **[BE-H6] + [BE-H7] + [BE-H8]** Add ownership checks to all 3 IDOR-vulnerable endpoints
12. **[FE-H1]** Implement `roleGuard` / `teacherGuard` / `adminGuard` in Angular routes
13. **[BE-H9]** Fix rate limiter IP extraction (trusted proxy allowlist only)
14. **[BE-C3]** Implement proper JWT revocation with `jti` claim + SHA-256 blocklist
15. **[FE-H2] + [BE-H2]** Remove JWT from WebSocket URL (use STOMP headers only)

### 📋 Medium-Term — Next Sprint

16. **[BE-M4]** Add per-account 2FA brute-force lockout
17. **[BE-M6]** Remove DevTools from production build
18. **[BE-M7]** Restrict Swagger UI to ADMIN role in production
19. **[FE-H5]** Add JWT expiry check to `isLoggedIn()` + 401 HTTP interceptor
20. **[BE-M8]** Invalidate reset tokens on password change
21. **[BE-M9]** Sanitize chat messages with OWASP Java HTML Sanitizer
22. **[FE-H8]** Move quiz state from `localStorage` to `sessionStorage`
23. **[FE-H3]** Add domain allowlist validation before `window.open()`
24. **[FE-H4]** Gate STOMP debug logging behind `environment.production`
25. All remaining MEDIUM, LOW, and INFO items

---

*Report generated by automated deep-dive security audit — Antigravity*
