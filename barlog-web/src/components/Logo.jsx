// A pull-up bar: two posts and a crossbar, with a small sage dot for the athlete.
export default function Logo({ size = 28 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path d="M5 28V8M27 28V8" stroke="#22302d" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M5 8h22" stroke="#5b8a78" strokeWidth="3.5" strokeLinecap="round" />
      <circle cx="16" cy="18" r="3.5" fill="#8fa9bd" />
    </svg>
  );
}
