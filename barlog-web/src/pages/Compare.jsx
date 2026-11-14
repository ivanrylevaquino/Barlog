import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { fmtDate } from '../utils';
import { Empty, Feedback, PageHead, StatusPill, VideoBox } from '../components/ui.jsx';

const label = (s) => `${fmtDate(s.createdAt, { month: 'short', day: 'numeric', year: 'numeric' })}, ${s.reps} reps, ${s.progression}`;
const count = (s, type) => s.video.analysis.items.filter((i) => i.type === type).length;

export default function Compare() {
  const [params] = useSearchParams();
  const [sets, setSets] = useState(null);
  const [exId, setExId] = useState(null);
  const [aId, setAId] = useState(params.get('a'));
  const [bId, setBId] = useState(null);
  const refA = useRef(); const refB = useRef();

  useEffect(() => {
    api.listVideoSets().then((list) => {
      setSets(list);
      const first = list.find((s) => s.id === params.get('a')) || list[0];
      if (first) setExId(first.exerciseId);
    });
  }, []);

  const pool = useMemo(() => (sets || []).filter((s) => s.exerciseId === exId), [sets, exId]);
  const exercises = useMemo(() => [...new Map((sets || []).map((s) => [s.exerciseId, s.exerciseName]))], [sets]);

  // default: earliest vs latest set of the chosen exercise
  useEffect(() => {
    if (!pool.length) return;
    if (!pool.some((s) => s.id === aId)) setAId(pool[pool.length - 1].id);
    if (!pool.some((s) => s.id === bId) || bId === aId) setBId(pool[0].id);
  }, [pool]);

  const a = pool.find((s) => s.id === aId);
  const b = pool.find((s) => s.id === bId);

  const playBoth = () => { [refA, refB].forEach((r) => r.current?.play?.()); };

  if (!sets) return <p className="muted">Loading…</p>;
  if (sets.length < 2) return (
    <div className="fade"><PageHead title="Compare" /><div className="card"><Empty title="Not enough videos yet" text="Film two sets of the same exercise to compare them here." to="/workout" action="Start workout" /></div></div>
  );

  const diff = a && b && a.id !== b.id ? [
    `${b.reps - a.reps >= 0 ? '+' : ''}${b.reps - a.reps} reps compared with the left set.`,
    `Warnings and errors: ${count(a, 'WARNING') + count(a, 'ERROR')} on the left, ${count(b, 'WARNING') + count(b, 'ERROR')} on the right.`,
    `Good points: ${count(a, 'POSITIVE')} on the left, ${count(b, 'POSITIVE')} on the right.`,
  ] : null;

  return (
    <div className="stack fade">
      <PageHead title="Compare" sub="Put two sets next to each other to see what changed.">
        <select aria-label="Exercise" style={{ maxWidth: 220 }} value={exId ?? ''} onChange={(e) => { setExId(Number(e.target.value)); }}>
          {exercises.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
        </select>
      </PageHead>

      <div className="compare">
        {[[a, aId, setAId, refA, 'Earlier set'], [b, bId, setBId, refB, 'Recent set']].map(([s, id, setId, ref, title]) => (
          <section key={title} className="card stack-sm">
            <div className="field">
              <label htmlFor={title}>{title}</label>
              <select id={title} value={id ?? ''} onChange={(e) => setId(e.target.value)}>
                {pool.map((p) => <option key={p.id} value={p.id}>{label(p)}</option>)}
              </select>
            </div>
            {s && <>
              <VideoBox video={s.video} innerRef={ref} />
              <div className="row between"><span className="muted small">{s.video.analysis.repsDetected} reps detected</span><StatusPill video={s.video} /></div>
              <Feedback video={s.video} />
            </>}
          </section>
        ))}
      </div>

      <div className="row">
        <button className="btn" onClick={playBoth}>Play both</button>
      </div>
      {diff && (
        <section className="card stack-sm">
          <h2>What changed</h2>
          {diff.map((d) => <p key={d}>{d}</p>)}
        </section>
      )}
    </div>
  );
}
