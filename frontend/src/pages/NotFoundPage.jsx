import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function NotFoundPage() {
  const { homeRoute } = useAuth();

  return (
    <div className="d-flex justify-content-center py-5">
      <div className="surface-card p-4 p-md-5 text-center" style={{ maxWidth: 520 }}>
        <div className="metric-chip d-inline-flex mb-3">Navigation</div>
        <h1 className="display-5">404</h1>
        <p className="text-muted">The page you requested could not be found.</p>
        <Link to={homeRoute} className="btn btn-modern btn-modern-primary">Go to Home</Link>
      </div>
    </div>
  );
}
