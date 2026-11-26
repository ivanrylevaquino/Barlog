import { Minus, Plus } from 'lucide-react';

export function Stepper({ label, value, onChange, step = 1, min = 0, unit }) {
  return (
    <div className="field">
      <span className="label">{label}{unit ? ` (${unit})` : ''}</span>
      <div className="stepper">
        <button type="button" aria-label={`Less ${label}`} onClick={() => onChange(Math.max(min, +(value - step).toFixed(1)))}><Minus size={18} /></button>
        <input type="number" aria-label={label} value={value} onChange={(e) => onChange(Number(e.target.value))} />
        <button type="button" aria-label={`More ${label}`} onClick={() => onChange(+(value + step).toFixed(1))}><Plus size={18} /></button>
      </div>
    </div>
  );
}

/** Resize a per-set list, filling new sets with the last value. */
export function resize(values, n, fill) {
  const out = values.slice(0, n);
  while (out.length < n) out.push(out.length ? out[out.length - 1] : fill);
  return out;
}

/**
 * Number of sets plus reps (or seconds) for each set.
 * value = { sets, same, values: number[] } ; "same" applies values[0] to every set.
 */
export default function PerSetInputs({ hold, value, onChange }) {
  const { sets, same, values } = value;
  const unitLabel = hold ? 'Seconds' : 'Reps';
  const step = hold ? 5 : 1;
  const setSets = (n) => {
    const count = Math.max(1, Math.min(50, Math.round(n) || 1));
    onChange({ ...value, sets: count, values: resize(values, count, hold ? 30 : 8) });
  };
  const setOne = (i, v) => {
    const next = same ? values.map(() => v) : values.map((x, j) => (j === i ? v : x));
    onChange({ ...value, values: next });
  };

  return (
    <div className="stack-sm">
      <div className="form-grid">
        <Stepper label="Sets" value={sets} min={1} onChange={setSets} />
        {same && <Stepper label={`${unitLabel} per set`} value={values[0] ?? 0} min={1} step={step} onChange={(v) => setOne(0, v)} />}
      </div>
      <label className="row small" style={{ gap: 6 }}>
        <input type="checkbox" checked={same} onChange={(e) => {
          const on = e.target.checked;
          onChange({ ...value, same: on, values: on ? values.map(() => values[0]) : values });
        }} />
        Same {unitLabel.toLowerCase()} for every set
      </label>
      {!same && (
        <div className="per-set">
          {values.map((v, i) => (
            <label key={i} className="field">
              <span className="label">Set {i + 1}</span>
              <input type="number" min="1" value={v} aria-label={`${unitLabel} in set ${i + 1}`}
                     onChange={(e) => setOne(i, Number(e.target.value))} />
            </label>
          ))}
        </div>
      )}
    </div>
  );
}

/** Build the initial editor state from a saved entry. */
export function fromEntry(entry) {
  const values = entry.perSet?.length ? entry.perSet : [entry.hold ? entry.holdSeconds ?? 30 : entry.reps || 8];
  return { sets: values.length, same: values.every((v) => v === values[0]), values };
}

export function checkPerSet(value, hold) {
  const max = hold ? 3600 : 500;
  if (value.values.some((v) => !Number.isFinite(v) || v < 1 || v > max || (!hold && !Number.isInteger(v)))) {
    return hold ? 'Each set needs a hold between 1 and 3600 seconds.' : 'Each set needs a whole number of reps from 1 to 500.';
  }
  return '';
}
