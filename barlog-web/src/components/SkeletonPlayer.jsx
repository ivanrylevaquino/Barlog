import { useEffect, useRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

// COCO-17 skeleton connections (pose model keypoint order).
const BONES = [[5, 7], [7, 9], [6, 8], [8, 10], [5, 6], [5, 11], [6, 12], [11, 12], [11, 13], [13, 15], [12, 14], [14, 16], [0, 5], [0, 6]];
const MIN_CONF = 0.4;
const toSeconds = (t) => { const [m, s] = String(t || '0:00').split(':').map(Number); return m * 60 + s; };

function frameAt(frames, t) {
  let lo = 0, hi = frames.length - 1, best = -1;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    if (frames[mid].t <= t) { best = mid; lo = mid + 1; } else hi = mid - 1;
  }
  return best >= 0 ? frames[best] : null;
}

/**
 * Video with the detected skeleton drawn on top, plus a live rep counter / hold timer and the form tip
 * for the current moment. Works without analysis too (plain video).
 */
export default function SkeletonPlayer({ src, analysis }) {
  const video = useRef();
  const canvas = useRef();
  const [show, setShow] = useState(true);
  const [now, setNow] = useState(0);
  const track = analysis?.track;

  useEffect(() => {
    let raf, last = -1;
    const draw = () => {
      raf = requestAnimationFrame(draw);
      const v = video.current, c = canvas.current;
      if (!v || !c) return;
      if (Math.abs(v.currentTime - last) > 0.05) { last = v.currentTime; setNow(last); }
      const w = v.clientWidth, h = v.clientHeight, dpr = window.devicePixelRatio || 1;
      if (c.width !== w * dpr || c.height !== h * dpr) { c.width = w * dpr; c.height = h * dpr; }
      const ctx = c.getContext('2d');
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, w, h);
      if (!show || !track?.frames?.length || !v.videoWidth) return;
      const f = frameAt(track.frames, v.currentTime);
      if (!f?.k) return;
      // object-fit: contain -> where the picture actually sits inside the element
      const scale = Math.min(w / v.videoWidth, h / v.videoHeight);
      const ox = (w - v.videoWidth * scale) / 2, oy = (h - v.videoHeight * scale) / 2;
      const sx = scale * v.videoWidth / (track.width || v.videoWidth);
      const sy = scale * v.videoHeight / (track.height || v.videoHeight);
      const P = (i) => [ox + f.k[i][0] * sx, oy + f.k[i][1] * sy];
      ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.strokeStyle = 'rgba(120, 220, 180, 0.9)';
      BONES.forEach(([a, b]) => {
        if (f.k[a][2] < MIN_CONF || f.k[b][2] < MIN_CONF) return;
        const [x1, y1] = P(a), [x2, y2] = P(b);
        ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
      });
      ctx.fillStyle = '#fff';
      f.k.forEach((p, i) => {
        if (p[2] < MIN_CONF || (i > 0 && i < 5)) return; // skip eyes/ears
        const [x, y] = P(i);
        ctx.beginPath(); ctx.arc(x, y, 3.5, 0, Math.PI * 2); ctx.fill();
      });
    };
    draw();
    return () => cancelAnimationFrame(raf);
  }, [track, show]);

  const reps = (analysis?.repEvents || []).filter((e) => e.counted && e.t <= now).length;
  const halves = (analysis?.repEvents || []).filter((e) => !e.counted && e.t <= now).length;
  const hw = analysis?.holdWindow;
  const held = hw ? Math.max(0, Math.min(now, hw.end) - hw.start) : null;
  const tip = (analysis?.items || []).filter((i) => i.type !== 'POSITIVE' && toSeconds(i.t) <= now && now - toSeconds(i.t) < 3).pop();

  return (
    <div className="skel">
      <video ref={video} src={src} controls playsInline preload="metadata" />
      <canvas ref={canvas} aria-hidden="true" />
      {analysis?.status === 'COMPLETED' && (
        <div className="skel-badge">
          {hw ? <><b>{held.toFixed(1)}s</b> held</> : <><b>{reps}</b> {reps === 1 ? 'rep' : 'reps'}{halves ? <small> +{halves} half</small> : null}</>}
        </div>
      )}
      {tip && <div className={`skel-tip ${tip.type}`}>{tip.text}</div>}
      {track && (
        <button type="button" className="skel-toggle" onClick={() => setShow(!show)} aria-label={show ? 'Hide skeleton' : 'Show skeleton'}>
          {show ? <EyeOff size={16} /> : <Eye size={16} />} {show ? 'Hide skeleton' : 'Show skeleton'}
        </button>
      )}
    </div>
  );
}
