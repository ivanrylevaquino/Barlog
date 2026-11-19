import { Navigate, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../auth.jsx';
import Logo from '../components/Logo.jsx';

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (user) return <Navigate to="/" replace />;

  const onSuccess = async (res) => {
    setBusy(true); setError('');
    try {
      await login(res.credential);
      nav('/');
    } catch (e) {
      setError('Invalid or expired Google sign-in. Please try again.');
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
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <GoogleLogin
            onSuccess={onSuccess}
            onError={() => setError('Google sign-in failed. Please try again.')}
            theme="filled_black"
            shape="pill"
          />
        </div>
        {busy && <p className="muted">Signing in…</p>}
        {error && <p className="error">{error}</p>}
      </div>
    </div>
  );
}