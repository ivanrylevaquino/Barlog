// Shared vocabulary for the post-session journal.

export const MOODS = [
  { value: 'DRAINED', word: 'Drained', line: 'Running on empty', color: '#a05555' },
  { value: 'TIRED', word: 'Tired', line: 'Worn out, but here', color: '#b0853a' },
  { value: 'OKAY', word: 'Okay', line: 'Got it done', color: '#8fa9bd' },
  { value: 'GOOD', word: 'Good', line: 'Felt solid', color: '#5b8a78' },
  { value: 'GREAT', word: 'Great', line: 'On fire today', color: '#3f6b59' },
];
export const moodOf = (v) => MOODS.find((m) => m.value === v);

export const TAGS = [
  ['STRONG', 'strong'], ['PUMPED', 'pumped'], ['FOCUSED', 'focused'], ['PROUD', 'proud'], ['LIGHT', 'light'],
  ['SORE', 'sore'], ['TIGHT', 'tight'], ['HEAVY', 'heavy'], ['DISTRACTED', 'distracted'], ['FRUSTRATED', 'frustrated'], ['PAIN', 'in pain'],
];
export const tagLabel = (v) => (TAGS.find(([k]) => k === v) || [v, v.toLowerCase()])[1];
export const MAX_TAGS = 6;

export const ENERGY = ['Empty tank', 'Low', 'Some left', 'Plenty left', 'Could go again'];

// Writing prompts, picked to match how the session felt.
const PROMPTS = {
  high: [
    'What clicked today?',
    'Which set are you proudest of, and why?',
    'What do you want to carry into the next session?',
    'Did anything feel easier than last time?',
  ],
  mid: [
    'What would make next time a little better?',
    'Was there a moment you almost stopped? What kept you going?',
    'How did your form feel on the last set?',
    'What is one small thing to work on next session?',
  ],
  low: [
    'What made today hard?',
    'Did sleep, food or stress play a part today?',
    'What is one thing you still did well?',
    'What does your body need before the next session?',
  ],
};
export function promptsFor(mood) {
  if (mood === 'GREAT' || mood === 'GOOD') return PROMPTS.high;
  if (mood === 'DRAINED' || mood === 'TIRED') return PROMPTS.low;
  return PROMPTS.mid;
}

export const journalDate = (iso) =>
  new Date(iso).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
export const journalTime = (iso) => new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });

export function recapLine(recap) {
  if (!recap) return '';
  const ex = recap.exercises.join(', ');
  return `${ex}${ex ? ' · ' : ''}${recap.sets} ${recap.sets === 1 ? 'set' : 'sets'} · ${recap.reps} reps`;
}
