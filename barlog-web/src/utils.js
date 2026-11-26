export const MAX_VIDEO_MB = 100; // keep in sync with your backend limit
export const VIDEO_TYPES = ['video/mp4', 'video/quicktime', 'video/webm'];

export const fmtDate = (iso, opts = { month: 'short', day: 'numeric' }) =>
  new Date(iso).toLocaleDateString(undefined, opts);

export const fmtLong = (iso) => fmtDate(iso, { weekday: 'long', month: 'long', day: 'numeric' });

export const toDisplayWeight = (kg, units) => (units === 'lb' ? Math.round(kg * 2.20462) : kg);
export const toKg = (value, units) => (units === 'lb' ? Math.round((value / 2.20462) * 10) / 10 : value);

// FR-012: type, non-empty, size
export function validateVideo(file) {
  if (!file) return 'Choose a video first.';
  if (!VIDEO_TYPES.includes(file.type)) return 'Use an MP4, MOV or WebM video.';
  if (file.size === 0) return 'That file is empty.';
  if (file.size > MAX_VIDEO_MB * 1024 * 1024) return `Video is over ${MAX_VIDEO_MB} MB. Trim it and try again.`;
  return '';
}

export const PROGRESSIONS = {
  PUSH_UP: ['Incline', 'Standard', 'Diamond', 'Archer'],
  PULL_UP: ['Negative', 'Assisted', 'Strict', 'Weighted'],
  SQUAT: ['Assisted', 'Bodyweight', 'Split squat', 'Pistol'],
  DIP: ['Bench', 'Assisted', 'Parallel bar', 'Weighted'],
};
export const progressionsFor = (ex) => ex?.progressions || PROGRESSIONS[ex?.type] || ['Standard'];

// "3 × 8 · Incline · +5 kg" or "2 × 45s · Forearm"
export function entryLabel(s, units) {
  const isHold = s.hold || s.holdSeconds != null;
  const fmt = (v) => (isHold ? `${v}s` : `${v}`);
  const values = s.perSet?.length ? s.perSet : Array(s.sets || 1).fill(isHold ? s.holdSeconds ?? 0 : s.reps);
  const work = values.every((v) => v === values[0]) ? `${values.length} × ${fmt(values[0])}` : values.map(fmt).join(', ');
  const weight = Number(s.weightKg) ? ` · +${toDisplayWeight(Number(s.weightKg), units)} ${units}` : '';
  return `${work} · ${s.progression}${weight}`;
}
