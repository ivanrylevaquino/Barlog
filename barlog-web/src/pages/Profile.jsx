import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { PageHead } from '../components/ui.jsx';

export default function Profile() {
  const { user, units, setUnits, logout } = useAuth();
  const nav = useNavigate();
  const initials = user.displayName.split(' ').map((w) => w[0]).join('').slice(0, 2);

  return (
    <div className="fade" style={{ maxWidth: 520 }}>
      <PageHead title="Profile" />
      <div className="card stack">
        <div className="row">
          {user.avatarUrl
            ? <img src={user.avatarUrl} alt="" width={56} height={56} style={{ borderRadius: '50%' }} />
            : <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--accent-soft)', color: 'var(--accent-ink)', display: 'grid', placeItems: 'center', fontWeight: 700 }}>{initials}</div>}
          <div><div className="title">{user.displayName}</div><div className="muted small">{user.email}</div></div>
        </div>
        <div className="field">
          <span className="label">Weight units</span>
          <div className="seg" role="group" aria-label="Weight units" style={{ alignSelf: 'flex-start' }}>
            {['kg', 'lb'].map((u) => <button key={u} aria-pressed={units === u} onClick={() => setUnits(u)}>{u}</button>)}
          </div>
        </div>
        <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={() => { logout(); nav('/login'); }}>Log out</button>
      </div>
    </div>
  );
}
