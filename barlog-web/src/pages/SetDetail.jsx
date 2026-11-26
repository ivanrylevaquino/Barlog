import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { entryLabel, fmtLong, progressionsFor, toDisplayWeight, toKg } from '../utils';
import { Feedback, StatusPill, VideoBox, VideoDrop, validateVideo } from '../components/ui.jsx';
import SkeletonPlayer from '../components/SkeletonPlayer.jsx';
import PerSetInputs, { checkPerSet, fromEntry, resize } from '../components/PerSetInputs.jsx';

export default function SetDetail() {
  const { setId } = useParams();
  const { units } = useAuth();
  const [set, setSet] = useState(null);
  const [error, setError] = useState('');
  const nav = useNavigate();
  const [exercise, setExercise] = useState(null);
  const [allEx, setAllEx] = useState([]);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [reload, setReload] = useState(0);

  const upload = async () => {
    const v = validateVideo(file);
    if (v) return setFormError(v);
    setUploading(true); setFormError('');
    try {
      await api.uploadVideo(set.id, file);
      setFile(null);
      setReload(reload + 1);
    } catch (err) { setFormError(err.message || 'Upload failed. Try again.'); }
    finally { setUploading(false); }
  };

  const retry = async () => {
    try { await api.reanalyzeVideo(set.video.id); setReload(reload + 1); }
    catch (err) { setFormError(err.message || 'Could not restart the analysis.'); }
  };

  useEffect(() => {
    if (set && !allEx.length) api.getExercises().then((l) => { setAllEx(l); setExercise(l.find((e) => e.id === set.exerciseId)); }).catch(() => {});
  }, [set]);

  const startEdit = () => {
    setForm({ exerciseId: set.exerciseId, progression: set.progression, work: fromEntry(set),
              weight: toDisplayWeight(Number(set.weightKg), units), rpe: set.rpe ?? '', notes: set.notes || '' });
    setFormError('');
    setEditing(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setFormError('');
    const ex = allEx.find((x) => x.id === Number(form.exerciseId)) || exercise;
    const weight = Number(form.weight), rpe = form.rpe === '' ? null : Number(form.rpe);
    const perSetError = checkPerSet(form.work, !!ex?.hold);
    if (perSetError) return setFormError(perSetError);
    if (Number.isNaN(weight) || weight < 0) return setFormError('Added weight cannot be negative.');
    if (rpe !== null && (!Number.isInteger(rpe) || rpe < 1 || rpe > 10)) return setFormError('RPE must be between 1 and 10.');
    setSaving(true);
    try {
      const updated = await api.updateSet(set.id, {
        exerciseId: ex?.id ?? set.exerciseId, progression: form.progression, sets: form.work.sets,
        perSet: form.work.values.map((v) => Math.round(v)),
        weightKg: toKg(weight, units), rpe, notes: form.notes,
      });
      setSet({ ...set, ...updated, video: set.video });
      setExercise(ex);
      setEditing(false);
    } catch (err) { setFormError(err.message || 'Could not save changes.'); }
    finally { setSaving(false); }
  };

  const remove = async () => {
    if (!window.confirm('Delete this exercise from the workout? This cannot be undone.')) return;
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
  }, [setId, reload]);

  if (error) return <p className="error">{error} <Link to="/history">Back to history</Link></p>;
  if (!set) return <p className="muted">Loading…</p>;

  return (
    <div className="stack fade">
      <div>
        <Link to="/history" className="small">Back to history</Link>
        <h1 style={{ marginTop: 8 }}>{set.exerciseName}</h1>
        <p className="muted">{fmtLong(set.createdAt)}</p>
      </div>
      {set.video?.url ? <SkeletonPlayer src={set.video.url} analysis={set.video.analysis} /> : <VideoBox video={set.video} />}
      <div className="stats">
        <div className="stat"><span className="muted small">Sets</span><b>{set.sets || 1}</b></div>
        <div className="stat"><span className="muted small">{set.hold ? 'Seconds per set' : 'Reps per set'}</span>
          <b style={{ fontSize: '1.2rem', paddingTop: 6 }}>{(set.perSet || [set.hold ? set.holdSeconds : set.reps]).map((v) => (set.hold ? `${v}s` : v)).join(' · ')}</b></div>
        <div className="stat"><span className="muted small">Progression</span><b style={{ fontSize: '1.2rem', paddingTop: 6 }}>{set.progression}</b></div>
        <div className="stat"><span className="muted small">Added weight</span><b>{toDisplayWeight(set.weightKg, units)} {units}</b></div>
      </div>
      {(set.rpe || set.notes) && (
        <p className="muted">{set.rpe && `Effort: RPE ${set.rpe}. `}{set.notes}</p>
      )}
      {editing ? (
        <form className="card stack-sm" onSubmit={save} noValidate>
          <h2>Edit exercise</h2>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="ed-ex">Exercise</label>
              <select id="ed-ex" value={form.exerciseId} onChange={(e) => {
                const ex = allEx.find((x) => x.id === Number(e.target.value));
                const was = allEx.find((x) => x.id === Number(form.exerciseId));
                const work = !!was?.hold === !!ex?.hold ? form.work
                  : { ...form.work, same: true, values: resize([], form.work.sets, ex?.hold ? 30 : 8) };
                setForm({ ...form, exerciseId: Number(e.target.value), progression: progressionsFor(ex)[0], work });
              }}>
                {allEx.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor="ed-prog">Progression</label>
              <select id="ed-prog" value={form.progression} onChange={(e) => setForm({ ...form, progression: e.target.value })}>
                {progressionsFor(allEx.find((x) => x.id === Number(form.exerciseId)) || exercise || { progressions: [set.progression] }).map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
          </div>
          <PerSetInputs hold={!!allEx.find((x) => x.id === Number(form.exerciseId))?.hold} value={form.work}
                        onChange={(work) => setForm({ ...form, work })} />
          <div className="form-grid">
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
          <button className="btn" onClick={startEdit}>Edit</button>
          <button className="btn ghost" onClick={remove}>Delete</button>
          {formError && <p className="error">{formError}</p>}
        </div>
      )}
      <section className="card stack-sm">
        <div className="row between"><h2>Form feedback</h2><StatusPill video={set.video} /></div>
        <Feedback video={set.video} />
        {set.video?.status === 'FAILED' && set.video.analysis?.failureReason === 'SERVICE_UNAVAILABLE' && (
          <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={retry}>Analyze again</button>
        )}
        {api.videoUploadEnabled && (
          <div className="stack-sm" style={{ marginTop: 8 }}>
            <span className="label">{set.video ? 'Replace the video' : 'Add a video of this set'}</span>
            <VideoDrop file={file} onFile={(f) => { setFile(f); setFormError(f ? validateVideo(f) : ''); }} />
            {file && <button className="btn primary" style={{ alignSelf: 'flex-start' }} onClick={upload} disabled={uploading}>
              {uploading ? 'Uploading…' : 'Upload and analyze'}</button>}
          </div>
        )}
      </section>
      {set.video?.status === 'COMPLETED' && <Link className="btn" to={`/compare?a=${set.id}`}>Compare with another set</Link>}
    </div>
  );
}
