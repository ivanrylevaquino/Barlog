import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { fmtLong, progressionsFor, toDisplayWeight, toKg } from '../utils';
import { Feedback, StatusPill, VideoBox } from '../components/ui.jsx';

export default function SetDetail() {
  const { setId } = useParams();
  const { units } = useAuth();
  const [set, setSet] = useState(null);
  const [error, setError] = useState('');
  const nav = useNavigate();
  const [exercise, setExercise] = useState(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (set && !exercise) api.getExercises().then((l) => setExercise(l.find((e) => e.id === set.exerciseId))).catch(() => {});
  }, [set]);

  const startEdit = () => {
    setForm({ progression: set.progression, reps: set.reps, weight: toDisplayWeight(Number(set.weightKg), units), rpe: set.rpe ?? '', notes: set.notes || '' });
    setFormError('');
    setEditing(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setFormError('');
    const reps = Number(form.reps), weight = Number(form.weight), rpe = form.rpe === '' ? null : Number(form.rpe);
    if (!Number.isInteger(reps) || reps < 1 || reps > 500) return setFormError('Reps must be a whole number from 1 to 500.');
    if (Number.isNaN(weight) || weight < 0) return setFormError('Added weight cannot be negative.');
    if (rpe !== null && (!Number.isInteger(rpe) || rpe < 1 || rpe > 10)) return setFormError('RPE must be between 1 and 10.');
    setSaving(true);
    try {
      const updated = await api.updateSet(set.id, { exerciseId: set.exerciseId, progression: form.progression, reps, weightKg: toKg(weight, units), rpe, notes: form.notes });
      setSet({ ...set, ...updated });
      setEditing(false);
    } catch (err) { setFormError(err.message || 'Could not save changes.'); }
    finally { setSaving(false); }
  };

  const remove = async () => {
    if (!window.confirm('Delete this set? This cannot be undone.')) return;
    try {
      await api.deleteSet(set.id);
      nav('/history');
    } catch (err) { setFormError(err.message || 'Could not delete the set.'); }
  };

  useEffect(() => {
    let timer;
    const load = async () => {
      try {
        const s = await api.getSet(setId);
        setSet(s);
        // FR-015: analysis is async, so poll until it finishes
        if (s.video && (s.video.status === 'PROCESSING' || s.video.status === 'UPLOADED')) timer = setTimeout(load, 3000);
      } catch { setError('We could not find that set.'); }
    };
    load();
    return () => clearTimeout(timer);
  }, [setId]);

  if (error) return <p className="error">{error} <Link to="/history">Back to history</Link></p>;
  if (!set) return <p className="muted">Loading…</p>;

  return (
    <div className="stack fade">
      <div>
        <Link to="/history" className="small">Back to history</Link>
        <h1 style={{ marginTop: 8 }}>{set.exerciseName}, set {set.setNumber}</h1>
        <p className="muted">{fmtLong(set.createdAt)}</p>
      </div>
      <VideoBox video={set.video} />
      <div className="stats">
        <div className="stat"><span className="muted small">Reps</span><b>{set.reps}</b></div>
        <div className="stat"><span className="muted small">Progression</span><b style={{ fontSize: '1.2rem', paddingTop: 6 }}>{set.progression}</b></div>
        <div className="stat"><span className="muted small">Added weight</span><b>{toDisplayWeight(set.weightKg, units)} {units}</b></div>
      </div>
      {(set.rpe || set.notes) && (
        <p className="muted">{set.rpe && `Effort: RPE ${set.rpe}. `}{set.notes}</p>
      )}
      {editing ? (
        <form className="card stack-sm" onSubmit={save} noValidate>
          <h2>Edit set</h2>
          <div className="field">
            <label htmlFor="ed-prog">Progression</label>
            <select id="ed-prog" value={form.progression} onChange={(e) => setForm({ ...form, progression: e.target.value })}>
              {progressionsFor(exercise || { type: null, progressions: [set.progression] }).map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="ed-reps">Reps</label>
              <input id="ed-reps" type="number" min="1" max="500" value={form.reps} onChange={(e) => setForm({ ...form, reps: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="ed-w">Added weight ({units})</label>
              <input id="ed-w" type="number" min="0" step="0.5" value={form.weight} onChange={(e) => setForm({ ...form, weight: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="ed-rpe">RPE (1-10, optional)</label>
              <input id="ed-rpe" type="number" min="1" max="10" value={form.rpe} onChange={(e) => setForm({ ...form, rpe: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="ed-notes">Notes</label>
            <input id="ed-notes" type="text" maxLength={500} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
          {formError && <p className="error">{formError}</p>}
          <div className="row" style={{ gap: 8 }}>
            <button className="btn primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
            <button className="btn ghost" type="button" onClick={() => setEditing(false)}>Cancel</button>
          </div>
        </form>
      ) : (
        <div className="row" style={{ gap: 8 }}>
          <button className="btn" onClick={startEdit}>Edit set</button>
          <button className="btn ghost" onClick={remove}>Delete set</button>
          {formError && <p className="error">{formError}</p>}
        </div>
      )}
      <section className="card stack-sm">
        <div className="row between"><h2>Form feedback</h2><StatusPill video={set.video} /></div>
        <Feedback video={set.video} />
      </section>
      {set.video?.status === 'COMPLETED' && <Link className="btn" to={`/compare?a=${set.id}`}>Compare with another set</Link>}
    </div>
  );
}
