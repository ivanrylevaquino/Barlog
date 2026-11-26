import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Sparkles, Trash2 } from 'lucide-react';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { entryLabel, fmtLong, progressionsFor, toKg, validateVideo } from '../utils';
import { PageHead, StatusPill, VideoDrop } from '../components/ui.jsx';
import SkeletonPlayer from '../components/SkeletonPlayer.jsx';
import PerSetInputs, { Stepper, checkPerSet } from '../components/PerSetInputs.jsx';

const BLANK = { weight: 0, rpe: null, notes: '' };
const defaultWork = (hold) => ({ sets: 3, same: true, values: hold ? [30, 30, 30] : [8, 8, 8] });
const single = (v) => ({ sets: 1, same: true, values: [v] });

export default function Workout() {
  const nav = useNavigate();
  const { units } = useAuth();
  const [exercises, setExercises] = useState([]);
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [startNotes, setStartNotes] = useState('');

  const [exId, setExId] = useState(null);
  const [prog, setProg] = useState('');
  const [form, setForm] = useState(BLANK);
  const [work, setWork] = useState(defaultWork(false));
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  // Video-first draft: the entry is created as soon as a clip is dropped, then filled in by the analysis.
  const [draft, setDraft] = useState(null); // { setId, src, video }
  const [countHalf, setCountHalf] = useState(false);
  const poll = useRef();

  const exercise = exercises.find((e) => e.id === exId);
  const exercisesRef = useRef([]);
  exercisesRef.current = exercises;
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  useEffect(() => {
    (async () => {
      try {
        const list = await api.getExercises();
        exercisesRef.current = list;
        setExercises(list);
        pick(list[0]);
        setSession(await api.getActiveSession());
      } catch (e) {
        setError(e.message || 'Could not load your workout.');
      } finally { setLoading(false); }
    })();
    return () => clearTimeout(poll.current);
  }, []);

  const pick = (e, keepProg) => {
    if (!e) return;
    setExId((prev) => {
      const was = exercisesRef.current.find((x) => x.id === prev);
      if (!was || !!was.hold !== !!e.hold) setWork(defaultWork(e.hold));
      return e.id;
    });
    const stages = progressionsFor(e);
    setProg((p) => (keepProg && stages.includes(p) ? p : stages[0]));
  };

  const payload = () => ({
    exerciseId: exId, progression: prog, sets: work.sets,
    perSet: work.values.map((v) => Math.round(v)),
    weightKg: toKg(form.weight, units), rpe: form.rpe, notes: form.notes,
  });

  const check = () => checkPerSet(work, exercise?.hold) || (form.weight < 0 ? 'Added weight cannot be negative.' : '');

  const refresh = async () => setSession(await api.getSession(session.id));

  const start = async () => {
    setError('');
    try { setSession(await api.createSession({ notes: startNotes })); }
    catch (e) { setError(e.message || 'Could not start the workout.'); }
  };

  // ── Video first ────────────────────────────────────────────────────
  const applyAnalysis = (a, half = countHalf) => {
    if (!a || a.status !== 'COMPLETED') return;
    const detected = exercises.find((e) => e.type === a.exercise);
    if (detected) pick(detected, true);
    // one video = one set; the user adds more sets afterwards if they want
    if (a.holdSeconds != null) setWork(single(Math.max(1, Math.round(a.holdSeconds))));
    else setWork(single(Math.max(1, (a.repsDetected || 0) + (half ? a.partialReps || 0 : 0))));
  };

  const watch = (setId) => {
    clearTimeout(poll.current);
    const tick = async () => {
      try {
        const s = await api.getSet(setId);
        setDraft((d) => (d && d.setId === setId ? { ...d, video: s.video } : d));
        if (s.video && (s.video.status === 'PROCESSING' || s.video.status === 'UPLOADED')) poll.current = setTimeout(tick, 2000);
        else applyAnalysis(s.video?.analysis);
      } catch { /* entry deleted meanwhile */ }
    };
    tick();
  };

  const onVideo = async (file) => {
    setError('');
    if (!file) return;
    const v = validateVideo(file);
    if (v) return setError(v);
    const err = check();
    if (err) return setError(err);
    setSaving(true);
    try {
      const entry = await api.addSet(session.id, payload());       // placeholder values, replaced by the analysis
      const src = URL.createObjectURL(file);
      setDraft({ setId: entry.id, src, video: { status: 'PROCESSING' } });
      await api.uploadVideo(entry.id, file);
      await refresh();
      watch(entry.id);
    } catch (e) {
      setError(e.message || 'Could not upload the video.');
    } finally { setSaving(false); }
  };

  const toggleHalf = (on) => {
    setCountHalf(on);
    const a = draft?.video?.analysis;
    if (a?.status === 'COMPLETED' && a.holdSeconds == null) {
      const reps = Math.max(1, (a.repsDetected || 0) + (on ? a.partialReps || 0 : 0));
      setWork((w) => ({ ...w, values: w.values.map((v, i) => (i === 0 || w.same ? reps : v)) }));
    }
  };

  const discardDraft = async () => {
    clearTimeout(poll.current);
    try { await api.deleteSet(draft.setId); } catch { /* already gone */ }
    setDraft(null);
    await refresh();
  };

  // ── Save ───────────────────────────────────────────────────────────
  const save = async () => {
    setError('');
    const err = check();
    if (err) return setError(err);
    setSaving(true);
    try {
      if (draft) await api.updateSet(draft.setId, payload());
      else await api.addSet(session.id, payload());
      clearTimeout(poll.current);
      setDraft(null);
      setForm(BLANK);
      setWork(defaultWork(exercise?.hold));
      setCountHalf(false);
      await refresh();
    } catch (e) {
      setError(e.message || 'Could not save this exercise. Try again.');
    } finally { setSaving(false); }
  };

  const finish = async () => {
    setError('');
    try { await api.finishSession(session.id); nav(`/journal/${session.id}?new=1`); }
    catch (e) { setError(e.message || 'Could not finish the workout.'); }
  };

  const discard = async () => {
    if (!window.confirm('Discard this workout? This cannot be undone.')) return;
    try { await api.deleteSession(session.id); setSession(null); }
    catch (e) { setError(e.message || 'Could not discard the workout.'); }
  };

  const removeEntry = async (s) => {
    if (!window.confirm(`Remove ${s.exerciseName} from this workout?`)) return;
    try {
      await api.deleteSet(s.id);
      if (draft?.setId === s.id) setDraft(null);
      await refresh();
    } catch (e) { setError(e.message || 'Could not remove it.'); }
  };

  if (loading) return <p className="muted">Loading…</p>;

  if (!session) {
    return (
      <div className="fade">
        <PageHead title="Workout" sub="Start a session, then add each exercise as you go." />
        <div className="card stack-sm" style={{ maxWidth: 520 }}>
          <div className="field">
            <label htmlFor="sn">Session notes (optional)</label>
            <textarea id="sn" value={startNotes} maxLength={1000} onChange={(e) => setStartNotes(e.target.value)} placeholder="Upper body day, park bars" />
          </div>
          {error && <p className="error">{error}</p>}
          <button className="btn primary big" onClick={start}>Start workout</button>
        </div>
      </div>
    );
  }

  const a = draft?.video?.analysis;
  const analyzing = draft && (!draft.video || draft.video.status === 'PROCESSING' || draft.video.status === 'UPLOADED');
  const detectedName = a?.exercise && exercises.find((e) => e.type === a.exercise)?.name;

  return (
    <div className="stack fade">
      <PageHead title="Workout" sub={fmtLong(session.date)}>
        <div className="row" style={{ gap: 8 }}>
          {session.sets.length === 0 && <button className="btn ghost" onClick={discard}>Discard</button>}
          <button className="btn" onClick={finish} disabled={session.sets.length === 0 || !!draft}>Finish workout</button>
        </div>
      </PageHead>

      <section className="card stack">
        {/* Video first */}
        {draft ? (
          <div className="stack-sm">
            <SkeletonPlayer src={draft.src} analysis={a} />
            {analyzing && <p className="muted"><Sparkles size={16} /> Analyzing your video: finding the exercise, counting reps and checking form…</p>}
            {draft.video?.status === 'FAILED' && <p className="error">{a?.summary || 'We could not analyze this video.'} Fill in the details below yourself.</p>}
            {a?.status === 'COMPLETED' && (
              <div className="detect">
                <p>
                  <Sparkles size={16} /> Looks like <b>{detectedName || exercise?.name}</b>
                  {a.holdSeconds != null ? <> held for <b>{a.holdSeconds}s</b></> : <>, <b>{a.repsDetected}</b> full {a.repsDetected === 1 ? 'rep' : 'reps'}</>}
                  {a.partialReps ? <> and {a.partialReps} half {a.partialReps === 1 ? 'rep' : 'reps'}</> : null}.
                  <span className="muted"> Check it below and fix anything that's off.</span>
                </p>
                {a.holdSeconds == null && a.partialReps > 0 && (
                  <label className="row small" style={{ gap: 6 }}>
                    <input type="checkbox" checked={countHalf} onChange={(e) => toggleHalf(e.target.checked)} /> Count half reps
                  </label>
                )}
              </div>
            )}
            <button className="btn ghost small" style={{ alignSelf: 'flex-start' }} onClick={discardDraft}>Remove this video and entry</button>
          </div>
        ) : (
          <div className="field">
            <span className="label">Film your set and drop it here: we'll detect the exercise and count it for you (optional)</span>
            <VideoDrop file={null} onFile={onVideo} />
          </div>
        )}

        <div className="field">
          <span className="label">Exercise</span>
          <div className="chips">
            {exercises.map((e) => <button key={e.id} className="chip" aria-pressed={e.id === exId} onClick={() => pick(e)}>{e.name}</button>)}
          </div>
        </div>
        <div className="field">
          <span className="label">Progression</span>
          <div className="chips">
            {progressionsFor(exercise).map((p) => <button key={p} className="chip" aria-pressed={p === prog} onClick={() => setProg(p)}>{p}</button>)}
          </div>
        </div>
        <PerSetInputs hold={!!exercise?.hold} value={work} onChange={setWork} />
        <div className="form-grid">
          <Stepper label="Added weight" unit={units} value={form.weight} step={units === 'lb' ? 5 : 2.5} onChange={(v) => set('weight', v)} />
        </div>
        <div className="field">
          <span className="label">Effort (RPE, optional)</span>
          <div className="rpe">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => <button key={n} aria-pressed={form.rpe === n} onClick={() => set('rpe', form.rpe === n ? null : n)}>{n}</button>)}
          </div>
        </div>
        <div className="field">
          <label htmlFor="exnotes">Notes for this exercise (optional)</label>
          <input id="exnotes" type="text" maxLength={500} value={form.notes} onChange={(e) => set('notes', e.target.value)} placeholder="Felt easy, grip slipped…" />
        </div>
        {error && <p className="error">{error}</p>}
        <button className="btn primary big" onClick={save} disabled={saving || analyzing}>
          {saving ? 'Saving…' : draft ? 'Save exercise' : 'Add exercise'}
        </button>
      </section>

      <section className="card">
        <h2 style={{ marginBottom: 6 }}>This session</h2>
        {session.sets.length === 0 ? <p className="muted">Exercises you add will show up here.</p> : (
          <div className="list">
            {session.sets.map((s) => (
              <div className="list-item" key={s.id}>
                <Link to={`/sets/${s.id}`} style={{ color: 'inherit' }}>
                  <div className="title">{s.exerciseName}</div>
                  <div className="muted small">{entryLabel(s, units)}{s.notes ? ` · ${s.notes}` : ''}</div>
                </Link>
                <div className="row" style={{ gap: 8 }}>
                  {draft?.setId === s.id ? <span className="pill neutral">Editing</span> : <StatusPill video={s.video} />}
                  <button className="btn ghost" aria-label={`Remove ${s.exerciseName}`} onClick={() => removeEntry(s)}><Trash2 size={16} /></button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
