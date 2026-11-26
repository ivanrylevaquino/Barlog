import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Trash2, Video } from 'lucide-react';
import { api } from '../api';
import { useAuth } from '../auth.jsx';
import { fmtDate, toDisplayWeight } from '../utils';
import { moodOf } from '../journal';
import { Empty, PageHead } from '../components/ui.jsx';

export default function History() {
  const { units } = useAuth();
  const [sessions, setSessions] = useState(null);
  const [q, setQ] = useState('');
  const [error, setError] = useState('');
  useEffect(() => { api.listSessions().then(setSessions).catch((e) => setError(e.message || 'Could not load history.')); }, []);

  const remove = async (s) => {
    if (!window.confirm('Delete this whole session and all of its sets? This cannot be undone.')) return;
    try {
      await api.deleteSession(s.id);
      setSessions(sessions.filter((x) => x.id !== s.id));
    } catch (e) { setError(e.message || 'Could not delete the session.'); }
  };

  const shown = useMemo(() => {
    if (!sessions) return [];
    const t = q.trim().toLowerCase();
    if (!t) return sessions;
    return sessions.filter((s) =>
      s.notes.toLowerCase().includes(t) ||
      fmtDate(s.date, { month: 'long', day: 'numeric' }).toLowerCase().includes(t) ||
      s.sets.some((x) => x.exerciseName.toLowerCase().includes(t) || x.progression.toLowerCase().includes(t)));
  }, [sessions, q]);

  return (
    <div className="fade">
      <PageHead title="History" sub="Every session and set you have logged.">
        <input type="search" aria-label="Search history" style={{ maxWidth: 260 }} placeholder="Search exercise or note" value={q} onChange={(e) => setQ(e.target.value)} />
      </PageHead>
      {error && <p className="error">{error}</p>}
      {!sessions ? (!error && <p className="muted">Loading…</p>) : shown.length === 0 ? (
        <div className="card"><Empty title="Nothing found" text={q ? 'Try a different search.' : 'Log a workout and it will appear here.'} to={q ? null : '/workout'} action="Start workout" /></div>
      ) : (
        <div className="stack">
          {shown.map((s) => (
            <section key={s.id} className="card">
              <div className="row between">
                <h2>{fmtDate(s.date, { weekday: 'long', month: 'long', day: 'numeric' })}</h2>
                <div className="row" style={{ gap: 8 }}>
                  <span className="muted small">{s.sets.length} sets{!s.endedAt ? ' · in progress' : ''}</span>
                  <button className="btn ghost" aria-label="Delete session" onClick={() => remove(s)}><Trash2 size={16} /></button>
                </div>
              </div>
              {s.notes && <p className="muted small">{s.notes}</p>}
              {s.endedAt && (
                <Link to={`/journal/${s.id}`} className="small journal-link" style={{ '--mood': moodOf(s.mood)?.color }}>
                  {s.mood ? <><span className="mood-dot" aria-hidden="true" /> Felt {moodOf(s.mood).word.toLowerCase()} · read journal</> : 'How did this session feel? Write in your journal'}
                </Link>
              )}
              <div className="list" style={{ marginTop: 8 }}>
                {s.sets.map((x) => (
                  <Link key={x.id} to={`/sets/${x.id}`} className="list-item">
                    <div>
                      <div className="title">{x.exerciseName}, set {x.setNumber}</div>
                      <div className="muted small">{x.progression}, {x.reps} reps{x.weightKg ? `, +${toDisplayWeight(x.weightKg, units)} ${units}` : ''}</div>
                    </div>
                    {x.video && <span className="pill neutral"><Video size={14} /> Video</span>}
                  </Link>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
