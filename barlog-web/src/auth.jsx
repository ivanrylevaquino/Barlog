import { createContext, useContext, useEffect, useState } from 'react';
import { api } from './api';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const [units, setUnitsState] = useState(localStorage.getItem('barlog.units') || 'kg');

  useEffect(() => {
    if (!localStorage.getItem('barlog.token')) return setReady(true);
    api.getMe().then(setUser).catch(() => localStorage.removeItem('barlog.token')).finally(() => setReady(true));
  }, []);

  // Pass the Google ID token (credential) here. Backend returns { token, user }.
  const login = async (googleIdToken) => {
    const { token, user } = await api.loginWithGoogle(googleIdToken);
    localStorage.setItem('barlog.token', token);
    setUser(user);
  };

  const logout = async () => {
    try { await api.logout?.(); } catch { }
    localStorage.removeItem('barlog.token');
    localStorage.removeItem('barlog.activeSession');
    setUser(null);
  };

  const setUnits = (u) => {
    localStorage.setItem('barlog.units', u);
    setUnitsState(u);
  };

  return <AuthContext.Provider value={{ user, ready, login, logout, units, setUnits }}>{children}</AuthContext.Provider>;
}
