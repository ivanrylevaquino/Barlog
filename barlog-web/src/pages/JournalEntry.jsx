import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { RefreshCw } from 'lucide-react';
import { api } from '../api';
import { ENERGY, MAX_TAGS, MOODS, TAGS, journalDate, journalTime, moodOf, promptsFor, recapLine, tagLabel } from '../journal';

const MAX_BODY = 5000;

export default function JournalEntry() {
  const { sessionId } = useParams();
  const [params] = useSearchParams();
  const isNew = params.get('new') === '1';
  const nav = useNavigate();

  const [session, setSession] = useState(null);
  const [entry, setEntry] = useState(null);
  const [editing, setEditing] = useState(false);
  const [loadError, setLoadError] = useState('');

  const [mood, setMood] = useState(null);
  const [energy, setEnergy] = useState(null);
  const [tags, setTags] = useState([]);
  const [body, setBody] = useState('');
  const [promptIx, setPromptIx] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [s, e] = await Promise.all([api.getSession(sessionId), api.getJournal(sessionId)]);
        setSession(s);
        setEntry(e);
        setEditing(!e);
        if (e) fillForm(e);
      } catch (err) {
        setLoadError(err.message || 'We could not open this journal page.');
      }
    })();
  }, [sessionId]);

  const fillForm = (e) => {
    setMood(e.mood); setEnergy(e.energy ?? null); setTags(e.tags || []); setBody(e.body || '');
  };

  const prompts = useMemo(() => promptsFor(mood), [mood]);
  const prompt = prompts[promptIx % prompts.length];

  const recap = session && {
    exercises: [...new Set(session.sets.map((x) => x.exerciseName))],
    sets: session.sets.length,
    reps: session.sets.reduce((a, x) => a + x.reps, 0),
  };

  const toggleTag = (t) => {
    setError('');
    if (tags.includes(t)) return setTags(tags.filter((x) => x !== t));
    if (tags.length >= MAX_TAGS) return setError(`Pick up to ${MAX_TAGS} words.`);
    setTags([...tags, t]);
  };

  const save = async (e) => {
    e.preventDefault();
    setError('');
    if (!mood) return setError('Pick how you feel first.');
    if (body.length > MAX_BODY) return setError(`Keep it under ${MAX_BODY} characters.`);
    setSaving(true);
    try {
      const saved = await api.saveJournal(sessionId, { mood, energy, tags, body });
      setEntry(saved);
      setEditing(false);
    } catch (err) {
      setError(err.message || 'Could not save your entry. Try again.');
    } finally { setSaving(false); }
  };

  const tearOut = async () => {
    if (!window.confirm('Tear this page out of your journal? This cannot be undone.')) return;
    try {
      await api.deleteJournal(sessionId);
      nav('/journal');
    } catch (err) { setError(err.message || 'Could not delete this entry.'); }
  };

  if (loadError) return <p className="error">{loadError} <Link to="/journal">Back to journal</Link></p>;
  if (!session) return <p className="muted">Opening your journal…</p>;

  if (!session.endedAt) {
    return (
      <div className="paper fade">
        <p className="paper-date">{journalDate(session.date)}</p>
        <p className="paper-ink">This workout is still going. Finish it first, then come back and write about how it went.</p>
        <Link className="btn" to="/workout" style={{ marginTop: 18 }}>Back to workout</Link>
      </div>
    );
  }

  return (
    <div className="stack fade">
      <Link to={isNew ? '/history' : '/journal'} className="small">{isNew ? 'Skip for now' : 'Back to journal'}</Link>

      <article className="paper">
        <header className="paper-head">
          <p className="paper-date">{journalDate(session.date)}</p>
          <p className="paper-meta">{journalTime(session.date)} · {recapLine(recap)}</p>
        </header>

        {!editing && entry ? (
          <ReadView entry={entry} onEdit={() => { fillForm(entry); setEditing(true); }} onDelete={tearOut} error={error} />
        ) : (
          <form onSubmit={save} noValidate className="stack-sm">
            {isNew && <p className="paper-hello">Nice work. Before you close this, take a minute for yourself.</p>}

            <fieldset className="paper-q">
              <legend>How do you feel after this session?</legend>
              <div className="moods">
                {MOODS.map((m) => (
                  <button type="button" key={m.value} className="mood" aria-pressed={mood === m.value}
                          style={{ '--mood': m.color }} onClick={() => { setMood(m.value); setPromptIx(0); setError(''); }}>
                    <span className="mood-dot" aria-hidden="true" />
                    <b>{m.word}</b>
                    <small>{m.line}</small>
                  </button>
                ))}
              </div>
            </fieldset>

            <fieldset className="paper-q">
              <legend>How much is left in the tank? <span className="muted small">(optional)</span></legend>
              <div className="energy" role="group">
                {ENERGY.map((label, i) => (
                  <button type="button" key={label} aria-pressed={energy === i + 1} aria-label={label}
                          className={energy && i < energy ? 'on' : ''} onClick={() => setEnergy(energy === i + 1 ? null : i + 1)} />
                ))}
                <span className="muted small">{energy ? ENERGY[energy - 1] : 'Tap a bar'}</span>
              </div>
            </fieldset>

            <fieldset className="paper-q">
              <legend>A few words for your body <span className="muted small">(optional)</span></legend>
              <div className="chips">
                {TAGS.map(([v, l]) => (
                  <button type="button" key={v} className="chip" aria-pressed={tags.includes(v)} onClick={() => toggleTag(v)}>{l}</button>
                ))}
              </div>
            </fieldset>

            <div className="paper-q">
              <div className="row between">
                <label htmlFor="jbody" className="paper-prompt">{prompt}</label>
                <button type="button" className="btn ghost small" onClick={() => setPromptIx(promptIx + 1)}>
                  <RefreshCw size={14} /> Another question
                </button>
              </div>
              <textarea id="jbody" className="lined" value={body} maxLength={MAX_BODY}
                        onChange={(e) => setBody(e.target.value)} placeholder="Write whatever comes to mind…" />
              <p className="muted small" style={{ textAlign: 'right' }}>{body.length} / {MAX_BODY}</p>
            </div>

            {error && <p className="error">{error}</p>}
            <div className="row">
              <button className="btn primary" type="submit" disabled={saving}>{saving ? 'Saving…' : entry ? 'Save changes' : 'Save to journal'}</button>
              {entry && <button className="btn ghost" type="button" onClick={() => { setEditing(false); setError(''); }}>Cancel</button>}
            </div>
          </form>
        )}
      </article>
    </div>
  );
}

function ReadView({ entry, onEdit, onDelete, error }) {
  const m = moodOf(entry.mood);
  return (
    <div className="stack-sm">
      <p className="paper-feel" style={{ '--mood': m?.color }}>
        <span className="mood-dot" aria-hidden="true" /> Felt <b>{m?.word.toLowerCase()}</b>
        {entry.energy ? <> · {ENERGY[entry.energy - 1].toLowerCase()}</> : null}
      </p>
      {entry.tags.length > 0 && <p className="paper-tags">{entry.tags.map(tagLabel).join(' · ')}</p>}
      {entry.body
        ? <div className="paper-ink">{entry.body.split(/\n+/).map((p, i) => <p key={i}>{p}</p>)}</div>
        : <p className="paper-ink muted">No words this time, and that’s fine.</p>}
      <p className="muted small">
        Written {journalDate(entry.createdAt)}{entry.updatedAt !== entry.createdAt ? ` · edited ${journalDate(entry.updatedAt)}` : ''}
      </p>
      {error && <p className="error">{error}</p>}
      <div className="row">
        <button className="btn" onClick={onEdit}>Edit entry</button>
        <button className="btn ghost" onClick={onDelete}>Tear out page</button>
      </div>
    </div>
  );
}
