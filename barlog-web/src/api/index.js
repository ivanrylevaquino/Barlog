// ─────────────────────────────────────────────────────────────
// The ONE place the UI talks to a backend.
// Every page calls `api.something()`. Set VITE_USE_MOCK=false in .env
// and the `real` object below is used instead of the fake data.
// Endpoint paths are suggestions: rename them to match your controllers.
// ─────────────────────────────────────────────────────────────
import * as mock from './mock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';
const BASE = import.meta.env.VITE_API_URL || '/api/v1';

async function http(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('barlog.token');
  const res = await fetch(BASE + path, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    localStorage.removeItem('barlog.token');
    window.location.href = '/login';
  }
  if (!res.ok) throw new Error((await res.text()) || `Request failed (${res.status})`);
  return res.status === 204 ? null : res.json();
}

const qs = (o) => {
  const p = new URLSearchParams(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''));
  return p.toString() ? `?${p}` : '';
};

const real = {
  // FR-001..003: send the Google ID token, get back { token, user }
  loginWithGoogle: (idToken) => http('/auth/google', { method: 'POST', body: { idToken } }),
  getMe: () => http('/users/me'),

  // FR-006: [{ id, name, type: 'PUSH_UP'|'PULL_UP'|'SQUAT'|'DIP', category, analysisSupported }]
  getExercises: () => http('/exercises'),

  // FR-005
  listSessions: () => http('/sessions'),
  getSession: (id) => http(`/sessions/${id}`),
  createSession: ({ notes }) => http('/sessions', { method: 'POST', body: { notes } }),
  finishSession: (id) => http(`/sessions/${id}/finish`, { method: 'PATCH' }),

  // FR-007..009: { exerciseId, progression, reps, weightKg, rpe, notes }
  addSet: (sessionId, set) => http(`/sessions/${sessionId}/sets`, { method: 'POST', body: set }),
  getSet: (setId) => http(`/sets/${setId}`),

  // FR-013/014: 1) get presigned PUT  2) PUT file straight to S3  3) tell backend it landed
  async uploadVideo(setId, file) {
    const { uploadUrl, videoId } = await http(`/sets/${setId}/video/upload-url`, {
      method: 'POST',
      body: { fileName: file.name, contentType: file.type, sizeBytes: file.size },
    });
    const put = await fetch(uploadUrl, { method: 'PUT', headers: { 'Content-Type': file.type }, body: file });
    if (!put.ok) throw new Error('Upload to storage failed.');
    return http(`/videos/${videoId}/confirm`, { method: 'POST' }); // backend then dispatches analysis (FR-015)
  },

  // Sets that have analyzed videos, newest first. Used by Compare.
  listVideoSets: () => http('/sets?hasVideo=true'),

  // FR-021..023 -> { totals: { volume, maxReps, sessions }, points: [{ date, volume, maxReps, sessions }] }
  getProgress: ({ range, exerciseId }) => http(`/progress${qs({ range, exerciseId })}`),
};

export const api = USE_MOCK ? mock : real;
