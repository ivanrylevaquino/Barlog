import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Workout from './pages/Workout.jsx';
import History from './pages/History.jsx';
import SetDetail from './pages/SetDetail.jsx';
import Progress from './pages/Progress.jsx';
import Compare from './pages/Compare.jsx';
import Profile from './pages/Profile.jsx';
import Journal from './pages/Journal.jsx';
import JournalEntry from './pages/JournalEntry.jsx';

function Protected({ children }) {
  const { user, ready } = useAuth();
  if (!ready) return <div className="splash">Loading…</div>;
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Protected><Layout /></Protected>}>
        <Route index element={<Dashboard />} />
        <Route path="workout" element={<Workout />} />
        <Route path="history" element={<History />} />
        <Route path="sets/:setId" element={<SetDetail />} />
        <Route path="journal" element={<Journal />} />
        <Route path="journal/:sessionId" element={<JournalEntry />} />
        <Route path="progress" element={<Progress />} />
        <Route path="compare" element={<Compare />} />
        <Route path="profile" element={<Profile />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
