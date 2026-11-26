# BarLog: Calisthenics Workout Logging and Form Analysis System

IT342 Project — User Accounts and Authentication (Increments 1 & 2) and Workout Logging, History and Progress Analytics (Increment 3).

## Tech Stack
- **Frontend:** React (Vite), React Router, `@react-oauth/google`, Recharts, Lucide React
- **Backend:** Spring Boot 3.3, Spring Security, Spring Data JPA, Java 17+
- **Database:** PostgreSQL (hosted on Supabase; H2 in-memory for `test` profile)
- **Auth:** Google OAuth 2.0 (ID token), Email + Password (BCrypt), and JWT (HS256, 24h)

## Authentication & Account Features
- **Google OAuth 2.0:** First Google sign-in creates a user record (`google_id`, `email`, `display_name`, `avatar_url`) with role `PRACTITIONER` and verifies the Google ID token on the backend before issuing a signed JWT.
- **Email & Password Login:** Users can sign in with email and password (`POST /api/auth/login`). Google-authenticated users can set a password from their Profile page to enable email/password login.
- **Profile & Password Management:** Authenticated users can update their display name (`PATCH /api/auth/me`), set or change their password (`PATCH /api/auth/me/password`), and toggle preferred weight units (`KG` / `LB` via `PATCH /api/auth/me/preferences`).
- **Password Reset:** Supports requesting and applying reset tokens (`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`).
- **Server-Side Logout:** Revokes the active JWT in `revoked_tokens`, so reusing a logged-out token returns `401`.
- **Role-Based Access:** `/api/dashboard/practitioner` requires `ROLE_PRACTITIONER`; all `/api/**` routes (except auth login/reset endpoints and `/error`) require authentication.

## Prerequisites
- JDK 17 or newer (note: if running tests with Mockito on Java 25+, pass `-DargLine="-Dnet.bytebuddy.experimental=true"`)
- Node.js 18 or newer
- A PostgreSQL database (Supabase Session Pooler or local PostgreSQL)
- A Google OAuth 2.0 Web Client ID

## Setup & Launch

### 1. Google OAuth
In Google Cloud Console, create an OAuth 2.0 Client ID (**Web application**) with authorized JavaScript origin `http://localhost:5173`. Add your Google account as a test user on the consent screen.

### 2. Backend (`barlog-back`)
Configure your database, Google Client ID, and JWT secret either via environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GOOGLE_CLIENT_ID`, `JWT_SECRET`) or directly in `barlog-back/src/main/resources/application.properties` (do not commit real secrets):

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
export SPRING_DATASOURCE_USERNAME="postgres.<project-ref>"
export SPRING_DATASOURCE_PASSWORD="<db-password>"
export GOOGLE_CLIENT_ID="<your-client-id>.apps.googleusercontent.com"
export JWT_SECRET="$(openssl rand -base64 32)"
```

Run the backend (tables are created/updated automatically via Hibernate `ddl-auto=update`):

```bash
cd barlog-back
./mvnw spring-boot:run
```

*(On Windows PowerShell, use `.\mvnw.cmd spring-boot:run`.)*

To run the backend test suite (uses H2 in-memory `test` profile):

```bash
cd barlog-back
./mvnw clean test
```

The API runs at `http://localhost:8080`.

### 3. Frontend (`barlog-web`)
Create `barlog-web/.env` (see `barlog-web/.env.example`):

```env
VITE_API_URL=http://localhost:8080/api
VITE_USE_MOCK=false
VITE_GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
```

Run the dev server:

```bash
cd barlog-web
npm install
npm run dev
```

Open `http://localhost:5173`.

## API Endpoints
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/google` | Public | Verify Google ID token, create or find user, return JWT and user |
| `POST` | `/api/auth/login` | Public | Authenticate with email and password, return JWT and user |
| `POST` | `/api/auth/forgot-password` | Public | Generate a password reset token |
| `POST` | `/api/auth/reset-password` | Public | Reset password using a valid reset token |
| `GET` | `/api/auth/me` | Bearer JWT | Get current user profile |
| `PATCH` | `/api/auth/me` | Bearer JWT | Update display name / avatar URL |
| `PATCH` | `/api/auth/me/password` | Bearer JWT | Set a new password or change existing password |
| `PATCH` | `/api/auth/me/preferences` | Bearer JWT | Update preferred weight unit (`KG` / `LB`) |
| `POST` | `/api/auth/logout` | Bearer JWT | Revoke the current JWT |
| `GET` | `/api/dashboard/practitioner` | `ROLE_PRACTITIONER` | Practitioner dashboard |
| `GET` | `/api/exercises` | `ROLE_PRACTITIONER` | Predefined exercise catalogue with progression stages (FR-006) |
| `GET` | `/api/sessions` | `ROLE_PRACTITIONER` | List own workout sessions with sets, newest first (FR-025) |
| `GET` | `/api/sessions/active` | `ROLE_PRACTITIONER` | The unfinished session, or `204` if none |
| `POST` | `/api/sessions` | `ROLE_PRACTITIONER` | Start a workout session (FR-005) |
| `GET` | `/api/sessions/{id}` | `ROLE_PRACTITIONER` | Get one session with its sets |
| `PATCH` | `/api/sessions/{id}` | `ROLE_PRACTITIONER` | Edit session notes |
| `PATCH` | `/api/sessions/{id}/finish` | `ROLE_PRACTITIONER` | Conclude a session (needs at least one set) |
| `DELETE` | `/api/sessions/{id}` | `ROLE_PRACTITIONER` | Delete a session and its sets |
| `POST` | `/api/sessions/{id}/sets` | `ROLE_PRACTITIONER` | Log a set: exercise, progression, reps, added kg, RPE, notes (FR-007..009) |
| `GET` | `/api/sets/{id}` | `ROLE_PRACTITIONER` | Get one set |
| `PATCH` | `/api/sets/{id}` | `ROLE_PRACTITIONER` | Edit a set |
| `DELETE` | `/api/sets/{id}` | `ROLE_PRACTITIONER` | Delete a set (remaining sets are renumbered) |
| `GET` | `/api/progress?range=7\|30\|90\|all&exerciseId=` | `ROLE_PRACTITIONER` | Volume, max reps and session count per day (FR-021..023) |

All workout endpoints are scoped to the signed-in user; another user's session or set id returns `404`.

## Security Notes
- `.env` files, `barlog-back/.m2/`, and `node_modules/` are gitignored. Never commit database passwords, JWT secrets, or real `application.properties` credentials.
