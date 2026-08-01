import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoadingSpinner from '../components/LoadingSpinner';

export default function ProtectedRoute({ allowedRoles }) {
  const { authReady, isAuthenticated, role, homeRoute } = useAuth();

  if (!authReady) {
    return <LoadingSpinner message="Restoring session..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles?.length && !allowedRoles.includes(role)) {
    return <Navigate to={homeRoute} replace />;
  }

  return <Outlet />;
}
