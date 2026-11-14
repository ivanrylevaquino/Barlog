import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { fmtLong, toDisplayWeight } from '../utils';
import { Feedback, StatusPill, VideoBox } from '../components/ui.jsx';

export default function SetDetail() {
  const { setId } = useParams();
  const { units } = useAuth();
  const [set, setSet] = useState(null);
  const [error, setError] = useState('');

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
      <section className="card stack-sm">
        <div className="row between"><h2>Form feedback</h2><StatusPill video={set.video} /></div>
        <Feedback video={set.video} />
      </section>
      {set.video?.status === 'COMPLETED' && <Link className="btn" to={`/compare?a=${set.id}`}>Compare with another set</Link>}
    </div>
  );
}
