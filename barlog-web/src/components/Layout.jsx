import { NavLink, Outlet } from 'react-router-dom';
import { ArrowLeftRight, Dumbbell, History, Home, TrendingUp, User } from 'lucide-react';
import Logo from './Logo.jsx';

const NAV = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/workout', label: 'Workout', icon: Dumbbell },
  { to: '/history', label: 'History', icon: History },
  { to: '/progress', label: 'Progress', icon: TrendingUp },
  { to: '/compare', label: 'Compare', icon: ArrowLeftRight },
  { to: '/profile', label: 'Profile', icon: User },
];

export default function Layout() {
  return (
    <div className="shell">
      <aside className="side">
        <NavLink to="/" className="logo"><Logo /> BarLog</NavLink>
        <nav className="nav" aria-label="Main">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end}>
              <Icon size={20} strokeWidth={1.8} /> {label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="main"><Outlet /></main>
    </div>
  );
}
