import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { PageHead } from '../components/ui.jsx';

export default function Profile() {
  const { user, units, setUnits, updateProfile, changePassword, logout } = useAuth();
  const nav = useNavigate();
  const initials = (user.displayName || user.email || '?')
    .split(' ')
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  const [displayName, setDisplayName] = useState(user.displayName || '');
  const [profileMsg, setProfileMsg] = useState('');
  const [profileErr, setProfileErr] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwMsg, setPwMsg] = useState('');
  const [pwErr, setPwErr] = useState('');
  const [savingPw, setSavingPw] = useState(false);

  const onSaveProfile = async (e) => {
    e.preventDefault();
    setProfileMsg(''); setProfileErr('');
    if (!displayName.trim()) {
      setProfileErr('Display name cannot be empty.');
      return;
    }
    setSavingProfile(true);
    try {
      await updateProfile({ displayName: displayName.trim() });
      setProfileMsg('Profile updated.');
    } catch (err) {
      setProfileErr(err.message || 'Could not update profile.');
    } finally {
      setSavingProfile(false);
    }
  };

  const onSavePassword = async (e) => {
    e.preventDefault();
    setPwMsg(''); setPwErr('');
    if (user.hasPassword && !currentPassword) {
      setPwErr('Please enter your current password.');
      return;
    }
    if (newPassword.length < 6) {
      setPwErr('New password must be at least 6 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPwErr('New passwords do not match.');
      return;
    }
    setSavingPw(true);
    try {
      await changePassword(user.hasPassword ? currentPassword : null, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPwMsg(user.hasPassword ? 'Password updated.' : 'Password set. You can now sign in with email and password.');
    } catch (err) {
      setPwErr(err.message || 'Could not update password.');
    } finally {
      setSavingPw(false);
    }
  };

  return (
    <div className="fade stack" style={{ maxWidth: 520 }}>
      <PageHead title="Profile" />

      <div className="card stack">
        <div className="row">
          {user.avatarUrl
            ? <img src={user.avatarUrl} alt="" width={56} height={56} style={{ borderRadius: '50%' }} />
            : <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--accent-soft)', color: 'var(--accent-ink)', display: 'grid', placeItems: 'center', fontWeight: 700 }}>{initials}</div>}
          <div>
            <div className="title">{user.displayName}</div>
            <div className="muted small">{user.email}</div>
          </div>
        </div>

        <form onSubmit={onSaveProfile} className="stack-sm">
          <div className="field">
            <label htmlFor="profile-name">Display name</label>
            <input
              id="profile-name"
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              disabled={savingProfile}
            />
          </div>
          <div className="row">
            <button type="submit" className="btn primary" disabled={savingProfile}>
              {savingProfile ? 'Saving…' : 'Save profile'}
            </button>
            {profileMsg && <span className="success">{profileMsg}</span>}
            {profileErr && <span className="error">{profileErr}</span>}
          </div>
        </form>

        <div className="field">
          <span className="label">Weight units</span>
          <div className="seg" role="group" aria-label="Weight units" style={{ alignSelf: 'flex-start' }}>
            {['kg', 'lb'].map((u) => <button key={u} type="button" aria-pressed={units === u} onClick={() => setUnits(u)}>{u}</button>)}
          </div>
        </div>
      </div>

      <div className="card stack-sm">
        <h2>{user.hasPassword ? 'Change password' : 'Set password'}</h2>
        <p className="muted small">
          {user.hasPassword
            ? 'Update the password used to sign in with your email.'
            : 'Add a password so you can also sign in with your email and password.'}
        </p>
        <form onSubmit={onSavePassword} className="stack-sm">
          {user.hasPassword && (
            <div className="field">
              <label htmlFor="current-password">Current password</label>
              <input
                id="current-password"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                disabled={savingPw}
              />
            </div>
          )}
          <div className="field">
            <label htmlFor="new-password">New password</label>
            <input
              id="new-password"
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              disabled={savingPw}
            />
          </div>
          <div className="field">
            <label htmlFor="confirm-password">Confirm new password</label>
            <input
              id="confirm-password"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={savingPw}
            />
          </div>
          <div className="row">
            <button type="submit" className="btn primary" disabled={savingPw}>
              {savingPw ? 'Saving…' : user.hasPassword ? 'Change password' : 'Set password'}
            </button>
            {pwMsg && <span className="success">{pwMsg}</span>}
            {pwErr && <span className="error">{pwErr}</span>}
          </div>
        </form>
      </div>

      <div>
        <button className="btn" onClick={() => { logout(); nav('/login'); }}>Log out</button>
      </div>
    </div>
  );
}
