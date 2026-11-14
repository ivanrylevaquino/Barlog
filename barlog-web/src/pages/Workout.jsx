import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Minus, Plus } from 'lucide-react';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { progressionsFor, toDisplayWeight, toKg, validateVideo, fmtLong } from '../utils';
import { PageHead, VideoDrop, StatusPill } from '../components/ui.jsx';

const KEY = 'barlog.activeSession';

export default function Workout() {
  const nav = useNavigate();
  const { units } = useAuth();
  const [exercises, setExercises] = useState([]);
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [startNotes, setStartNotes] = useState('');

  const [exId, setExId] = useState(null);
  const [prog, setProg] = useState('');
  const [reps, setReps] = useState(8);
  const [weight, setWeight] = useState(0);
  const [rpe, setRpe] = useState(null);
  const [notes, setNotes] = useState('');
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const exercise = exercises.find((e) => e.id === exId);

  useEffect(() => {
    (async () => {
      const list = await api.getExercises();
      setExercises(list);
      setExId(list[0]?.id);
      setProg(progressionsFor(list[0])[0]);
      const id = localStorage.getItem(KEY);
      if (id) {
        try { setSession(await api.getSession(id)); } catch { localStorage.removeItem(KEY); }
      }
      setLoading(false);
    })();
  }, []);

  const pickExercise = (e) => { setExId(e.id); setProg(progressionsFor(e)[0]); };

  const start = async () => {
    const s = await api.createSession({ notes: startNotes });
    localStorage.setItem(KEY, s.id);
    setSession(s);
  };

  const logSet = async () => {
    setError('');
    if (reps < 1) return setError('Enter at least 1 rep.');
    if (file) { const v = validateVideo(file); if (v) return setError(v); }
    setSaving(true);
    try {
      const set = await api.addSet(session.id, { exerciseId: exId, progression: prog, reps, weightKg: toKg(weight, units), rpe, notes });
      if (file) await api.uploadVideo(set.id, file);
      setSession(await api.getSession(session.id));
      setFile(null); setNotes(''); setRpe(null);
    } catch (e) {
      setError('Could not save this set. Try again.');
    } finally { setSaving(false); }
  };

  const finish = async () => {
    await api.finishSession(session.id);
    localStorage.removeItem(KEY);
    nav('/history');
  };

  if (loading) return <p className="muted">Loading…</p>;

  if (!session) {
    return (
      <div className="fade">
        <PageHead title="Workout" sub="Start a session, then log each set as you go." />
        <div className="card stack-sm" style={{ maxWidth: 520 }}>
          <div className="field">
            <label htmlFor="sn">Session notes (optional)</label>
            <textarea id="sn" value={startNotes} onChange={(e) => setStartNotes(e.target.value)} placeholder="Upper body day, park bars" />
          </div>
          <button className="btn primary big" onClick={start}>Start workout</button>
        </div>
      </div>
    );
  }

  return (
    <div className="stack fade">
      <PageHead title="Workout" sub={fmtLong(session.date)}>
        <button className="btn" onClick={finish} disabled={session.sets.length === 0}>Finish workout</button>
      </PageHead>

      <section className="card stack">
        <div className="field">
          <span className="label">Exercise</span>
          <div className="chips">
            {exercises.map((e) => <button key={e.id} className="chip" aria-pressed={e.id === exId} onClick={() => pickExercise(e)}>{e.name}</button>)}
          </div>
        </div>
        <div className="field">
          <span className="label">Progression</span>
          <div className="chips">
            {progressionsFor(exercise).map((p) => <button key={p} className="chip" aria-pressed={p === prog} onClick={() => setProg(p)}>{p}</button>)}
          </div>
        </div>
        <div className="form-grid">
          <div className="field">
            <span className="label">Reps</span>
            <div className="stepper">
              <button aria-label="Fewer reps" onClick={() => setReps(Math.max(0, reps - 1))}><Minus size={18} /></button>
              <input type="number" aria-label="Reps" value={reps} onChange={(e) => setReps(Number(e.target.value))} />
              <button aria-label="More reps" onClick={() => setReps(reps + 1)}><Plus size={18} /></button>
            </div>
          </div>
          <div className="field">
            <span className="label">Added weight ({units})</span>
            <div className="stepper">
              <button aria-label="Less weight" onClick={() => setWeight(Math.max(0, weight - (units === 'lb' ? 5 : 2.5)))}><Minus size={18} /></button>
              <input type="number" aria-label="Added weight" value={weight} onChange={(e) => setWeight(Number(e.target.value))} />
              <button aria-label="More weight" onClick={() => setWeight(weight + (units === 'lb' ? 5 : 2.5))}><Plus size={18} /></button>
            </div>
          </div>
        </div>
        <div className="field">
          <span className="label">Effort (RPE, optional)</span>
          <div className="rpe">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => <button key={n} aria-pressed={rpe === n} onClick={() => setRpe(rpe === n ? null : n)}>{n}</button>)}
          </div>
        </div>
        <div className="field">
          <label htmlFor="setnotes">Set notes (optional)</label>
          <input id="setnotes" type="text" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Felt easy, grip slipped…" />
        </div>
        <div className="field">
          <span className="label">Form video (optional)</span>
          <VideoDrop file={file} onFile={(f) => { setFile(f); setError(f ? validateVideo(f) : ''); }} error={error && file ? error : ''} />
        </div>
        {error && !file && <p className="error">{error}</p>}
        <button className="btn primary big" onClick={logSet} disabled={saving}>{saving ? 'Saving…' : 'Log set'}</button>
      </section>

      <section className="card">
        <h2 style={{ marginBottom: 6 }}>This session</h2>
        {session.sets.length === 0 ? <p className="muted">Sets you log will show up here.</p> : (
          <div className="list">
            {session.sets.map((s) => (
              <div className="list-item" key={s.id}>
                <div>
                  <div className="title">{s.exerciseName}, set {s.setNumber}</div>
                  <div className="muted small">{s.progression}, {s.reps} reps{s.weightKg ? `, +${toDisplayWeight(s.weightKg, units)} ${units}` : ''}{s.rpe ? `, RPE ${s.rpe}` : ''}</div>
                </div>
                <StatusPill video={s.video} />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
