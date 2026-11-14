import { Link } from 'react-router-dom';
import { useRef } from 'react';
import { Play, UploadCloud, Check } from 'lucide-react';
import { validateVideo } from '../utils';

export const PageHead = ({ title, sub, children }) => (
  <div className="page-head">
    <div><h1>{title}</h1>{sub && <p>{sub}</p>}</div>
    {children}
  </div>
);

export const Empty = ({ title, text, to, action }) => (
  <div className="empty">
    <h3>{title}</h3>
    <p>{text}</p>
    {to && <Link className="btn primary" style={{ marginTop: 16 }} to={to}>{action}</Link>}
  </div>
);

const FAIL_TEXT = {
  POOR_LIGHTING: 'The video was too dark to read your movement.',
  OCCLUSION: 'Part of your body was hidden from the camera.',
  DETECTION_FAILED: 'We could not find your body in the video.',
};

export function StatusPill({ video }) {
  if (!video) return null;
  if (video.status === 'PROCESSING' || video.status === 'UPLOADED') return <span className="pill neutral">Analyzing…</span>;
  if (video.status === 'FAILED') return <span className="pill bad">Analysis failed</span>;
  const o = video.analysis?.overall;
  if (o === 'GOOD') return <span className="pill">Good form</span>;
  return <span className="pill warn">Needs work</span>;
}

export const VideoBox = ({ video, innerRef }) => (
  <div className="video">
    {video?.url ? (
      <video ref={innerRef} src={video.url} controls playsInline />
    ) : (
      <div><Play size={30} strokeWidth={1.6} /><p className="small" style={{ marginTop: 6 }}>{video ? 'Video preview' : 'No video for this set'}</p></div>
    )}
  </div>
);

export function Feedback({ video }) {
  if (!video) return <p className="muted">Attach a video to this set to get form feedback.</p>;
  if (video.status === 'PROCESSING' || video.status === 'UPLOADED') return <p className="muted">Analysis usually takes a minute or two. This page updates on its own.</p>;
  const a = video.analysis;
  if (video.status === 'FAILED') return <p className="error">{FAIL_TEXT[a?.failureReason] || a?.summary} Film again with your full body in frame and good light.</p>;
  return (
    <div className="stack-sm">
      <p>{a.summary}</p>
      <div className="feedback">
        {a.items.map((it, i) => (
          <div key={i} className={`fb ${it.type}`}>
            <div><small>{it.t}</small><p>{it.text}</p></div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function VideoDrop({ file, onFile, error }) {
  const input = useRef();
  const pick = (f) => onFile(f || null);
  return (
    <div className="stack-sm">
      <div
        className={`drop ${file ? 'has' : ''}`}
        onClick={() => input.current.click()}
        onDragOver={(e) => { e.preventDefault(); e.currentTarget.classList.add('over'); }}
        onDragLeave={(e) => e.currentTarget.classList.remove('over')}
        onDrop={(e) => { e.preventDefault(); e.currentTarget.classList.remove('over'); pick(e.dataTransfer.files[0]); }}
        role="button" tabIndex={0}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && input.current.click()}
      >
        {file ? <><Check size={18} /> {file.name}</> : <><UploadCloud size={20} /><p>Drop a video here or click to browse</p><p className="small">MP4, MOV or WebM, up to 100 MB</p></>}
        <input ref={input} type="file" accept="video/mp4,video/quicktime,video/webm" hidden onChange={(e) => pick(e.target.files[0])} />
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

export { validateVideo };
