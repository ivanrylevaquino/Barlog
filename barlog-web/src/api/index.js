// ─────────────────────────────────────────────────────────────
// The ONE place the UI talks to a backend.
// Every page calls `api.something()`. Set VITE_USE_MOCK=false in .env
// and the `real` object below is used instead of the fake data.
// Endpoint paths are suggestions: rename them to match your controllers.
// ─────────────────────────────────────────────────────────────
import * as mock from './mock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';
const BASE = import.meta.env.VITE_API_URL || '/api/v1';

async function http(path, { method = 'GET', body, form } = {}) {
  const token = localStorage.getItem('barlog.token');
  const res = await fetch(BASE + path, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: form || (body ? JSON.stringify(body) : undefined),
  });
  if (res.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/me/password')) {
    localStorage.removeItem('barlog.token');
    window.location.href = '/login';
  }
  if (!res.ok) {
    const text = await res.text();
    let message = text;
    try { message = JSON.parse(text).message || text; } catch { /* plain-text body */ }
    const err = new Error(message || `Request failed (${res.status})`);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

const qs = (o) => {
  const p = new URLSearchParams(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''));
  return p.toString() ? `?${p}` : '';
};

const real = {
  // FR-001..003: send the Google ID token or email/password, get back { token, user }
  loginWithGoogle: (idToken) => http('/auth/google', { method: 'POST', body: { idToken } }),
  loginWithPassword: ({ email, password }) => http('/auth/login', { method: 'POST', body: { email, password } }),
  getMe: () => http('/auth/me'),
  updateProfile: (data) => http('/auth/me', { method: 'PATCH', body: data }),
  changePassword: ({ currentPassword, newPassword }) => http('/auth/me/password', { method: 'PATCH', body: { currentPassword, newPassword } }),
  forgotPassword: (email) => http('/auth/forgot-password', { method: 'POST', body: { email } }),
  resetPassword: ({ token, newPassword }) => http('/auth/reset-password', { method: 'POST', body: { token, newPassword } }),
  logout: () => http('/auth/logout', { method: 'POST' }),

  // FR-006: [{ id, name, type: 'PUSH_UP'|'PULL_UP'|'SQUAT'|'DIP', category, analysisSupported }]
  getExercises: () => http('/exercises'),

  // FR-005
  listSessions: () => http('/sessions'),
  getActiveSession: () => http('/sessions/active'), // null (204) when no workout is in progress
  getSession: (id) => http(`/sessions/${id}`),
  createSession: ({ notes }) => http('/sessions', { method: 'POST', body: { notes } }),
  updateSession: (id, { notes }) => http(`/sessions/${id}`, { method: 'PATCH', body: { notes } }),
  finishSession: (id) => http(`/sessions/${id}/finish`, { method: 'PATCH' }),
  deleteSession: (id) => http(`/sessions/${id}`, { method: 'DELETE' }),

  // FR-007..009: { exerciseId, progression, reps, weightKg, rpe, notes }
  addSet: (sessionId, set) => http(`/sessions/${sessionId}/sets`, { method: 'POST', body: set }),
  getSet: (setId) => http(`/sets/${setId}`),
  updateSet: (setId, set) => http(`/sets/${setId}`, { method: 'PATCH', body: set }),
  deleteSet: (setId) => http(`/sets/${setId}`, { method: 'DELETE' }),

  // Post-session journal: { mood, energy, tags, body }
  listJournal: () => http('/journal'),
  getJournal: (sessionId) => http(`/sessions/${sessionId}/journal`), // null (204) when not written yet
  saveJournal: (sessionId, entry) => http(`/sessions/${sessionId}/journal`, { method: 'PUT', body: entry }),
  deleteJournal: (sessionId) => http(`/sessions/${sessionId}/journal`, { method: 'DELETE' }),

  // Video upload (FR-010..014) has no backend yet; the UI hides the upload box while this is false.
  videoUploadEnabled: true,

  // FR-011..015: upload the clip; the backend stores it and starts pose analysis in the background.
  uploadVideo(setId, file) {
    const form = new FormData();
    form.append('file', file);
    return http(`/sets/${setId}/video`, { method: 'POST', form });
  },
  reanalyzeVideo: (videoId) => http(`/videos/${videoId}/reanalyze`, { method: 'POST' }),

  // Sets that have analyzed videos, newest first. Used by Compare.
  listVideoSets: () => http('/sets?hasVideo=true'),

  // FR-021..023 -> { totals: { volume, maxReps, sessions }, points: [{ date, volume, maxReps, sessions }] }
  getProgress: ({ range, exerciseId }) => http(`/progress${qs({ range, exerciseId })}`),
};

export const api = USE_MOCK ? mock : real;
