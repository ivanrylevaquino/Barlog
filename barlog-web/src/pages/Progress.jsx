import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api';
import { fmtDate } from '../utils';
import { Empty, PageHead } from '../components/ui.jsx';

const RANGES = [[7, '7 days'], [30, '30 days'], [90, '90 days'], ['all', 'All time']];
const METRICS = { volume: 'Total reps', maxReps: 'Max reps', sessions: 'Sessions' };

export default function Progress() {
  const [range, setRange] = useState(30);
  const [exerciseId, setExerciseId] = useState('');
  const [metric, setMetric] = useState('volume');
  const [exercises, setExercises] = useState([]);
  const [data, setData] = useState(null);

  useEffect(() => { api.getExercises().then(setExercises); }, []);
  useEffect(() => { setData(null); api.getProgress({ range, exerciseId }).then(setData); }, [range, exerciseId]);

  const points = (data?.points || []).map((p) => ({ ...p, label: fmtDate(p.date) }));
  const axis = { stroke: '#6b7b77', fontSize: 12, tickLine: false, axisLine: false };

  return (
    <div className="stack fade">
      <PageHead title="Progress" sub="See how your volume and strength are trending." />
      <div className="row between">
        <div className="seg" role="group" aria-label="Time range">
          {RANGES.map(([v, l]) => <button key={v} aria-pressed={range === v} onClick={() => setRange(v)}>{l}</button>)}
        </div>
        <select aria-label="Exercise" style={{ maxWidth: 220 }} value={exerciseId} onChange={(e) => setExerciseId(e.target.value)}>
          <option value="">All exercises</option>
          {exercises.map((e) => <option key={e.id} value={e.id}>{e.name}</option>)}
        </select>
      </div>

      <div className="stats">
        <div className="stat"><span className="muted small">Total reps</span><b>{data?.totals.volume ?? '–'}</b></div>
        <div className="stat"><span className="muted small">Best set</span><b>{data?.totals.maxReps ?? '–'}</b></div>
        <div className="stat"><span className="muted small">Sessions</span><b>{data?.totals.sessions ?? '–'}</b></div>
      </div>

      <section className="card stack-sm">
        <div className="seg" role="group" aria-label="Metric" style={{ alignSelf: 'flex-start' }}>
          {Object.entries(METRICS).map(([k, l]) => <button key={k} aria-pressed={metric === k} onClick={() => setMetric(k)}>{l}</button>)}
        </div>
        {!data ? <p className="muted">Loading…</p> : points.length === 0 ? (
          <Empty title="No data in this range" text="Log a workout or pick a longer range." to="/workout" action="Start workout" />
        ) : (
          <div style={{ height: 300 }}>
            <ResponsiveContainer>
              {metric === 'sessions' ? (
                <BarChart data={points}>
                  <CartesianGrid stroke="#e8eeeb" vertical={false} />
                  <XAxis dataKey="label" {...axis} /><YAxis allowDecimals={false} {...axis} width={32} />
                  <Tooltip cursor={{ fill: '#f2f5f3' }} />
                  <Bar dataKey="sessions" name="Sessions" fill="#8fa9bd" radius={[6, 6, 0, 0]} />
                </BarChart>
              ) : (
                <LineChart data={points}>
                  <CartesianGrid stroke="#e8eeeb" vertical={false} />
                  <XAxis dataKey="label" {...axis} /><YAxis {...axis} width={32} />
                  <Tooltip />
                  <Line type="monotone" dataKey={metric} name={METRICS[metric]} stroke="#5b8a78" strokeWidth={2.5} dot={{ r: 3, fill: '#5b8a78' }} />
                </LineChart>
              )}
            </ResponsiveContainer>
          </div>
        )}
      </section>
    </div>
  );
}
