import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { HrDashboard } from './pages/HrDashboard';
import { SeekerDashboard } from './pages/SeekerDashboard';
import { HiringManagerDashboard } from './pages/HiringManagerDashboard';
import type { Role } from './api/types';

function RequireRole({ role, children }: { role: Role; children: React.ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/hr"
        element={
          <RequireRole role="HR">
            <HrDashboard />
          </RequireRole>
        }
      />
      <Route
        path="/seeker"
        element={
          <RequireRole role="JOB_SEEKER">
            <SeekerDashboard />
          </RequireRole>
        }
      />
      <Route
        path="/hiring-manager"
        element={
          <RequireRole role="HIRING_MANAGER">
            <HiringManagerDashboard />
          </RequireRole>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
