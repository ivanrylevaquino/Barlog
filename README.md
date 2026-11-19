@'
# BarLog: Calisthenics Workout Logging and Form Analysis System

IT342 Project Increment 1: User Accounts and Authentication.

## Tech Stack
- Frontend: React (Vite), React Router, @react-oauth/google
- Backend: Spring Boot 3.3, Spring Security, Spring Data JPA, Java 17+
- Database: PostgreSQL (hosted on Supabase)
- Auth: Google OAuth 2.0 (ID token) and JWT (HS256, 24h)

## Authentication Design (per SRS)
- Passwordless Google sign-in (FR-001, NFR-004). The system never asks for or stores passwords, so there is no password hashing.
- First login creates a user record (google_id, email, display_name, avatar_url) with role PRACTITIONER, the only role in the SRS (FR-002).
- The backend verifies the Google ID token, then issues a signed JWT (FR-003).
- Logout revokes the token server-side (revoked_tokens table), so reusing it returns 401 (FR-004).
- Role-based access: /api/dashboard/practitioner requires ROLE_PRACTITIONER. The frontend uses a protected route guard and the backend enforces the role.

## Prerequisites
- JDK 17 or newer
- Node.js 18 or newer
- A PostgreSQL database (Supabase or local)
- A Google OAuth Web Client ID

## Setup

### 1. Google OAuth
In Google Cloud Console create an OAuth client (Web application) with authorized JavaScript origin http://localhost:5173. Add your Google account as a Test user on the consent screen. Only the Client ID is needed; the client secret is not used.

### 2. Backend (back-bar/barlog-back)
Set these environment variables (PowerShell example):

    $env:SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
    $env:SPRING_DATASOURCE_USERNAME="<db user>"
    $env:SPRING_DATASOURCE_PASSWORD="<db password>"
    $env:GOOGLE_CLIENT_ID="<your client id>.apps.googleusercontent.com"
    $env:JWT_SECRET="<random base64 string, 32+ bytes>"

Generate a JWT secret with:

    [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))

Run the backend (tables are created automatically on first start):

    cd back-bar/barlog-back
    .\mvnw.cmd spring-boot:run

The API runs at http://localhost:8080.

### 3. Frontend (barlog-web)
Create barlog-web/.env (see .env.example):

    VITE_GOOGLE_CLIENT_ID=<your client id>.apps.googleusercontent.com
    VITE_API_URL=http://localhost:8080/api
    VITE_USE_MOCK=false

Run:

    cd barlog-web
    npm install
    npm run dev

Open http://localhost:5173.

## API Endpoints
| Method | Path | Description |
|---|---|---|
| POST | /api/auth/google | Verify Google ID token, create or find user, return JWT and user |
| GET | /api/auth/me | Current user profile |
| PATCH | /api/auth/me/preferences | Update measurement units (kg/lb) |
| POST | /api/auth/logout | Revoke the current JWT |
| GET | /api/dashboard/practitioner | Practitioner dashboard (role protected) |

## Testing the Flow
1. Sign in with Google and you land on the dashboard; a row appears in the users table.
2. Refresh the page and the session persists.
3. Log out and you return to /login; a row appears in revoked_tokens.
4. Open /dashboard while logged out and you are redirected to /login.

## Security Notes
- Secrets are read from environment variables and are never committed. .env files are gitignored.
- Other increments (workout logging, video upload, form analysis, analytics) are not part of this increment.
'@ | Set-Content README.md