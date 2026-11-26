// Fake backend: lives in memory, resets on refresh. Same function names as `real` in index.js.
const wait = (ms = 250) => new Promise((r) => setTimeout(r, ms));
const uid = () => Math.random().toString(36).slice(2, 10);
const daysAgo = (n, h = 18) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  d.setHours(h, 0, 0, 0);
  return d.toISOString();
};

const EXERCISES = [
  { id: 1, name: 'Push-up', type: 'PUSH_UP', category: 'Push', analysisSupported: true },
  { id: 2, name: 'Pull-up', type: 'PULL_UP', category: 'Pull', analysisSupported: true },
  { id: 3, name: 'Squat', type: 'SQUAT', category: 'Legs', analysisSupported: true },
  { id: 4, name: 'Parallel bar dip', type: 'DIP', category: 'Push', analysisSupported: true },
];

const FEEDBACK = {
  1: {
    early: [['WARNING', '0:03', 'Your hips sag toward the floor. Squeeze your glutes to keep one straight line from shoulders to ankles.'], ['ERROR', '0:07', 'Depth is shallow. Lower until your elbows reach about 90°.'], ['POSITIVE', '0:10', 'You lock out fully at the top of each rep.']],
    late: [['POSITIVE', '0:02', 'Your body stays in one straight line through the whole set.'], ['POSITIVE', '0:06', 'Elbows reach 90° on every rep.'], ['WARNING', '0:12', 'Elbows flare a little on the last two reps. Tuck them closer to your ribs when tired.']],
  },
  2: {
    early: [['ERROR', '0:04', 'Your chin does not clear the bar on most reps. Pull until it is above the bar.'], ['WARNING', '0:09', 'Legs swing on the way up. Keep your core tight and legs still.'], ['POSITIVE', '0:12', 'You start each rep from a full dead hang.']],
    late: [['POSITIVE', '0:03', 'Chin clears the bar on every rep.'], ['POSITIVE', '0:08', 'You lower slowly and stay in control.'], ['WARNING', '0:14', 'A small swing shows up on the final rep.']],
  },
  3: {
    early: [['WARNING', '0:02', 'Knees drift inward as you stand up. Push them out over your toes.'], ['WARNING', '0:06', 'Heels lift slightly at the bottom. Keep your whole foot on the floor.'], ['POSITIVE', '0:09', 'Your chest stays upright.']],
    late: [['POSITIVE', '0:03', 'Hips drop below your knees on every rep.'], ['POSITIVE', '0:07', 'Knees track over your toes.'], ['POSITIVE', '0:11', 'Heels stay planted.']],
  },
  4: {
    early: [['WARNING', '0:03', 'Shoulders roll forward at the bottom. Keep your chest proud.'], ['ERROR', '0:08', 'You stop short of 90° elbow bend. Lower a little further.'], ['POSITIVE', '0:11', 'Full lockout at the top.']],
    late: [['POSITIVE', '0:04', 'Elbows reach 90° with steady control.'], ['POSITIVE', '0:09', 'Shoulders stay stable and down.'], ['WARNING', '0:13', 'Torso leans forward more as you tire.']],
  },
};

const buildAnalysis = (exId, good, reps) => ({
  overall: good ? 'GOOD' : 'NEEDS_IMPROVEMENT',
  repsDetected: reps,
  summary: good
    ? 'Solid set. Your form held up well, with one small thing to watch.'
    : 'A few fixable form issues showed up. Work on the first tip next session.',
  items: (good ? FEEDBACK[exId].late : FEEDBACK[exId].early).map(([type, t, text]) => ({ type, t, text })),
});

// ---- seed data ----
const PROG = { 1: ['Incline', 'Standard', 'Standard', 'Diamond'], 2: ['Negative', 'Assisted', 'Strict', 'Strict'], 3: ['Bodyweight', 'Bodyweight', 'Split squat', 'Split squat'], 4: ['Bench', 'Assisted', 'Parallel bar', 'Parallel bar'] };
let sessions = [];
for (let i = 0; i < 12; i++) {
  const age = 58 - i * 5;
  const progress = i / 11;
  const ids = i % 2 === 0 ? [1, 2] : [3, 4, 1];
  const sets = [];
  ids.forEach((exId) => {
    for (let n = 1; n <= 3; n++) {
      const reps = Math.round(5 + progress * 8 + (exId === 2 ? -2 : 0) - (n - 1));
      const hasVideo = n === 1;
      const failed = hasVideo && i === 3 && exId === 1;
      const stage = PROG[exId][Math.min(3, Math.floor(progress * 4))];
      sets.push({
        id: uid(), exerciseId: exId, exerciseName: EXERCISES[exId - 1].name, setNumber: n, reps,
        progression: stage, weightKg: 0, rpe: 6 + (n - 1), notes: '', createdAt: daysAgo(age, 18),
        video: hasVideo
          ? {
              id: uid(), url: null,
              status: failed ? 'FAILED' : 'COMPLETED',
              analysis: failed
                ? { overall: 'FAILED', failureReason: 'POOR_LIGHTING', repsDetected: 0, summary: 'We could not track your body. Film in a brighter spot with your full body in frame.', items: [] }
                : buildAnalysis(exId, progress > 0.45, reps),
            }
          : null,
      });
    }
  });
  sessions.push({ id: uid(), date: daysAgo(age), startedAt: daysAgo(age, 18), endedAt: daysAgo(age, 19), notes: i === 11 ? 'Felt strong today.' : '', sets });
}
sessions.sort((a, b) => new Date(b.date) - new Date(a.date));
sessions.forEach((s) => s.sets.forEach((x) => (x.sessionId = s.id, x.sessionDate = s.date)));

const allSets = () => sessions.flatMap((s) => s.sets);
const findSet = (id) => allSets().find((s) => s.id === id);

// ---- api ----
let mockUser = { id: 'u1', displayName: 'Ivan Aquino', email: 'ivan@example.com', avatarUrl: null, hasPassword: false };

export async function loginWithGoogle() {
  await wait();
  return { token: 'mock-jwt', user: await getMe() };
}
export async function loginWithPassword({ email }) {
  await wait();
  return { token: 'mock-jwt', user: { ...mockUser, email: email || mockUser.email } };
}
export async function getMe() {
  await wait(100);
  return { ...mockUser };
}
export async function updateProfile(data) {
  await wait(150);
  mockUser = { ...mockUser, ...data };
  return { ...mockUser };
}
export async function changePassword() {
  await wait(150);
  mockUser = { ...mockUser, hasPassword: true };
  return { token: 'mock-jwt', user: { ...mockUser } };
}
export const videoUploadEnabled = true;

// ---- journal ----
let journal = {}; // sessionId -> entry
const journalDto = (sessionId) => {
  const s = sessions.find((x) => x.id === sessionId);
  const e = journal[sessionId];
  return e && s ? {
    ...e, sessionId, sessionDate: s.date,
    recap: { exercises: [...new Set(s.sets.map((x) => x.exerciseName))], sets: s.sets.length, reps: s.sets.reduce((a, x) => a + x.reps, 0) },
  } : null;
};
export async function listJournal() {
  await wait();
  return sessions.filter((s) => journal[s.id]).map((s) => journalDto(s.id));
}
export async function getJournal(sessionId) { await wait(100); return journalDto(sessionId); }
export async function saveJournal(sessionId, { mood, energy, tags, body }) {
  await wait(200);
  const s = sessions.find((x) => x.id === sessionId);
  if (!s.endedAt) throw new Error('Finish the workout before writing about it.');
  const now = new Date().toISOString();
  journal[sessionId] = { id: journal[sessionId]?.id || uid(), mood, energy, tags: tags || [], body: (body || '').trim(), createdAt: journal[sessionId]?.createdAt || now, updatedAt: now };
  s.mood = mood;
  return journalDto(sessionId);
}
export async function deleteJournal(sessionId) {
  await wait(100);
  delete journal[sessionId];
  const s = sessions.find((x) => x.id === sessionId);
  if (s) s.mood = null;
  return null;
}
export async function getActiveSession() {
  await wait(100);
  return sessions.find((s) => !s.endedAt) || null;
}
export async function updateSession(id, { notes }) {
  await wait(100);
  const s = sessions.find((x) => x.id === id);
  s.notes = notes || '';
  return s;
}
export async function deleteSession(id) {
  await wait(100);
  sessions = sessions.filter((x) => x.id !== id);
  return null;
}
export async function updateSet(id, data) {
  await wait(100);
  const set = findSet(id);
  Object.assign(set, { reps: data.reps, progression: data.progression, weightKg: data.weightKg || 0, rpe: data.rpe, notes: data.notes || '' });
  return { ...set };
}
export async function deleteSet(id) {
  await wait(100);
  const s = sessions.find((x) => x.sets.some((y) => y.id === id));
  const gone = s.sets.find((y) => y.id === id);
  s.sets = s.sets.filter((y) => y.id !== id);
  let n = 1;
  s.sets.forEach((y) => { if (y.exerciseId === gone.exerciseId) y.setNumber = n++; });
  return null;
}
export async function getExercises() { await wait(100); return EXERCISES; }
export async function listSessions() { await wait(); return sessions; }
export async function getSession(id) {
  await wait(100);
  const s = sessions.find((x) => x.id === id);
  if (!s) throw new Error('Session not found');
  return s;
}
export async function createSession({ notes }) {
  await wait();
  const now = new Date().toISOString();
  const s = { id: uid(), date: now, startedAt: now, endedAt: null, notes: notes || '', sets: [] };
  sessions = [s, ...sessions];
  return s;
}
export async function finishSession(id) {
  await wait();
  const s = sessions.find((x) => x.id === id);
  if (s) s.endedAt = new Date().toISOString();
  return s;
}
export async function addSet(sessionId, data) {
  await wait(150);
  const s = sessions.find((x) => x.id === sessionId);
  const ex = EXERCISES.find((e) => e.id === data.exerciseId);
  const set = {
    id: uid(), sessionId, sessionDate: s.date, exerciseId: ex.id, exerciseName: ex.name,
    setNumber: s.sets.filter((x) => x.exerciseId === ex.id).length + 1,
    reps: data.reps, progression: data.progression, weightKg: data.weightKg || 0, rpe: data.rpe, notes: data.notes || '',
    createdAt: new Date().toISOString(), video: null,
  };
  s.sets.push(set);
  return set;
}
export async function getSet(id) {
  await wait(100);
  const set = findSet(id);
  if (!set) throw new Error('Set not found');
  return { ...set };
}
export async function uploadVideo(setId, file) {
  await wait(700);
  const set = findSet(setId);
  set.video = { id: uid(), url: URL.createObjectURL(file), status: 'PROCESSING', analysis: null };
  // pretend the Python service finishes after a few seconds
  setTimeout(() => {
    set.video.status = 'COMPLETED';
    set.video.analysis = buildAnalysis(set.exerciseId, Math.random() > 0.4, set.reps);
  }, 5000);
  return set.video;
}
export async function reanalyzeVideo() { await wait(100); return null; }
export async function listVideoSets() {
  await wait();
  return allSets().filter((s) => s.video && s.video.status === 'COMPLETED').sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}
export async function getProgress({ range = 30, exerciseId } = {}) {
  await wait();
  const cutoff = range === 'all' ? 0 : Date.now() - Number(range) * 864e5;
  const byDay = {};
  allSets().forEach((s) => {
    if (exerciseId && s.exerciseId !== Number(exerciseId)) return;
    if (new Date(s.sessionDate).getTime() < cutoff) return;
    const key = s.sessionDate.slice(0, 10);
    const d = (byDay[key] ||= { date: key, volume: 0, maxReps: 0, sessionIds: new Set() });
    d.volume += s.reps;
    d.maxReps = Math.max(d.maxReps, s.reps);
    d.sessionIds.add(s.sessionId);
  });
  const points = Object.values(byDay).sort((a, b) => a.date.localeCompare(b.date)).map(({ sessionIds, ...d }) => ({ ...d, sessions: sessionIds.size }));
  return {
    points,
    totals: {
      volume: points.reduce((a, p) => a + p.volume, 0),
      maxReps: Math.max(0, ...points.map((p) => p.maxReps)),
      sessions: points.reduce((a, p) => a + p.sessions, 0),
    },
  };
}
