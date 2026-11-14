# BarLog Web (React + Vite)

Minimal frontend for the BarLog SRS. Runs on fake data until you connect your Spring Boot API.

## Run
```bash
npm install
cp .env.example .env
npm run dev        # http://localhost:5173
```
Click "Continue with Google" on the login page (mock mode signs you in instantly).

## Connecting your backend
1. In `.env` set `VITE_USE_MOCK=false`.
2. Open `src/api/index.js`. The `real` object has one function per backend call, each with the
   SRS requirement it maps to. Rename paths / response fields to match your controllers.
3. `vite.config.js` proxies `/api` to `http://localhost:8080`, so no CORS setup is needed in dev.
4. Login: in `src/pages/Login.jsx`, swap the mock token for the real Google ID token
   (e.g. `@react-oauth/google`). The returned JWT is stored in `localStorage` and sent as `Authorization: Bearer`.

## Expected data shapes
| Call | Returns |
|---|---|
| `POST /auth/google` | `{ token, user: { id, displayName, email, avatarUrl } }` |
| `GET /exercises` | `[{ id, name, type: PUSH_UP/PULL_UP/SQUAT/DIP, category, analysisSupported }]` |
| `GET /sessions` | `[{ id, date, endedAt, notes, sets: [Set] }]` newest first |
| `Set` | `{ id, sessionId, sessionDate, exerciseId, exerciseName, setNumber, reps, progression, weightKg, rpe, notes, createdAt, video }` |
| `video` | `null` or `{ id, url (presigned GET), status: PROCESSING/COMPLETED/FAILED, analysis }` |
| `analysis` | `{ overall: GOOD/NEEDS_IMPROVEMENT/FAILED, repsDetected, summary, failureReason, items: [{ type: POSITIVE/WARNING/ERROR, t, text }] }` |
| `GET /progress?range=30&exerciseId=1` | `{ totals: { volume, maxReps, sessions }, points: [{ date, volume, maxReps, sessions }] }` |

The set detail page polls `GET /sets/:id` every 3 s while `video.status` is `PROCESSING`.

## Pages
`/login` · `/` home · `/workout` · `/history` · `/sets/:id` · `/progress` · `/compare` · `/profile`
