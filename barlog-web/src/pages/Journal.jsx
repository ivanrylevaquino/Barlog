import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { Empty, PageHead } from '../components/ui.jsx';
import { journalDate, moodOf, recapLine, tagLabel } from '../journal';

const monthOf = (iso) => new Date(iso).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

export default function Journal() {
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api.listJournal().then(setEntries).catch((e) => setError(e.message || 'Could not open your journal.')); }, []);

  const months = useMemo(() => {
    const out = [];
    (entries || []).forEach((e) => {
      const m = monthOf(e.sessionDate);
      if (!out.length || out[out.length - 1].month !== m) out.push({ month: m, items: [] });
      out[out.length - 1].items.push(e);
    });
    return out;
  }, [entries]);

  return (
    <div className="fade">
      <PageHead title="Journal" sub="How each session felt, in your own words." />
      {error && <p className="error">{error}</p>}
      {!entries ? (!error && <p className="muted">Opening your journal…</p>) : entries.length === 0 ? (
        <div className="card"><Empty title="Your journal is empty" text="Finish a workout and write a few lines about how it felt." to="/workout" action="Start workout" /></div>
      ) : (
        <div className="stack">
          {months.map(({ month, items }) => (
            <section key={month} className="stack-sm">
              <h2 className="journal-month">{month}</h2>
              {items.map((e) => {
                const m = moodOf(e.mood);
                return (
                  <Link key={e.id} to={`/journal/${e.sessionId}`} className="paper paper-snippet">
                    <div className="row between">
                      <span className="paper-date small-date">{journalDate(e.sessionDate)}</span>
                      <span className="paper-feel" style={{ '--mood': m?.color }}><span className="mood-dot" aria-hidden="true" /> {m?.word}</span>
                    </div>
                    <p className="paper-meta">{recapLine(e.recap)}</p>
                    {e.body
                      ? <p className="paper-ink clamp">{e.body}</p>
                      : <p className="paper-ink muted">No words this time.</p>}
                    {e.tags.length > 0 && <p className="paper-tags">{e.tags.map(tagLabel).join(' · ')}</p>}
                  </Link>
                );
              })}
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
