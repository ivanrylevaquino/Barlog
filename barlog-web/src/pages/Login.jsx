import { Navigate, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../auth.jsx';
import Logo from '../components/Logo.jsx';

export default function Login() {
  const { user, login, loginWithPassword } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (user) return <Navigate to="/" replace />;

  const onSuccess = async (res) => {
    setBusy(true); setError('');
    try {
      await login(res.credential);
      nav('/');
    } catch {
      setError('Invalid or expired Google sign-in. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const onPasswordSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please enter your email and password.');
      return;
    }
    setBusy(true); setError('');
    try {
      await loginWithPassword(email.trim(), password);
      nav('/');
    } catch {
      setError('Invalid email or password.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login">
      <div className="login-card fade">
        <Logo size={56} />
        <h1>BarLog</h1>
        <p className="muted">Log your sets, film them, and see how your form changes over time.</p>

        <form onSubmit={onPasswordSubmit} className="stack-sm" style={{ width: '100%', textAlign: 'left' }}>
          <div className="field">
            <label htmlFor="login-email">Email</label>
            <input
              id="login-email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={busy}
            />
          </div>
          <div className="field">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={busy}
            />
          </div>
          <button type="submit" className="btn primary" style={{ width: '100%', marginTop: 4 }} disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="row" style={{ width: '100%', color: 'var(--muted)', fontSize: '0.85rem' }}>
          <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
          <span>or</span>
          <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
        </div>

        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <GoogleLogin
            onSuccess={onSuccess}
            onError={() => setError('Google sign-in failed. Please try again.')}
            theme="filled_black"
            shape="pill"
          />
        </div>
        {error && <p className="error">{error}</p>}
      </div>
    </div>
  );
}
