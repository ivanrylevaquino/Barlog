import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { fmtDate } from '../utils';
import { Empty, StatusPill } from '../components/ui.jsx';

export default function Dashboard() {
  const { user } = useAuth();
  const [sessions, setSessions] = useState(null);
  useEffect(() => { api.listSessions().then(setSessions); }, []);

  const weekAgo = Date.now() - 7 * 864e5;
  const week = (sessions || []).filter((s) => new Date(s.date) > weekAgo);
  const weekSets = week.flatMap((s) => s.sets);
  const latestVideoSet = (sessions || []).flatMap((s) => s.sets).find((s) => s.video?.status === 'COMPLETED');

  return (
    <div className="stack fade">
      <div className="page-head" style={{ marginBottom: 0 }}>
        <div>
          <h1>Hi, {user.displayName.split(' ')[0]}</h1>
          <p>Ready for today’s session?</p>
        </div>
        <Link className="btn primary big" to="/workout">Start workout</Link>
      </div>

      <div className="stats">
        <div className="stat"><span className="muted small">Sessions this week</span><b>{week.length}</b></div>
        <div className="stat"><span className="muted small">Reps this week</span><b>{weekSets.reduce((a, s) => a + s.reps, 0)}</b></div>
        <div className="stat"><span className="muted small">Sets filmed</span><b>{weekSets.filter((s) => s.video).length}</b></div>
      </div>

      <section className="card">
        <div className="row between" style={{ marginBottom: 8 }}>
          <h2>Recent sessions</h2>
          <Link to="/history" className="small">See all</Link>
        </div>
        {!sessions ? <p className="muted">Loading…</p> : sessions.length === 0 ? (
          <Empty title="No sessions yet" text="Log your first workout to see it here." to="/workout" action="Start workout" />
        ) : (
          <div className="list">
            {sessions.slice(0, 4).map((s) => (
              <div key={s.id} className="list-item">
                <div>
                  <div className="title">{fmtDate(s.date, { weekday: 'long', month: 'short', day: 'numeric' })}</div>
                  <div className="muted small">{[...new Set(s.sets.map((x) => x.exerciseName))].join(', ') || 'No sets'}</div>
                </div>
                <span className="muted small">{s.sets.length} sets</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {latestVideoSet && (
        <section className="card">
          <div className="row between" style={{ marginBottom: 6 }}>
            <h2>Latest form check</h2>
            <StatusPill video={latestVideoSet.video} />
          </div>
          <p className="muted" style={{ marginBottom: 10 }}>{latestVideoSet.exerciseName}, {fmtDate(latestVideoSet.createdAt)}</p>
          <p>{latestVideoSet.video.analysis.summary}</p>
          <Link className="small" to={`/sets/${latestVideoSet.id}`}>Open set</Link>
        </section>
      )}
    </div>
  );
}
