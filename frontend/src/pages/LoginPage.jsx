import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaEye, FaEyeSlash, FaEnvelope, FaLock } from 'react-icons/fa6';
import { useAuth } from '../hooks/useAuth';
import { getHomeRouteByRole } from '../utils/accessControl';
import { parseApiError } from '../utils/apiError';
import { validateLogin } from '../utils/validation';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);
  const [values, setValues] = useState({ username: '', password: '' });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);

  const onSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateLogin(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    try {
      setLoading(true);
      const result = await login(values);
      toast.success('Logged in successfully');
      navigate(getHomeRouteByRole(result.role), { replace: true });
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card card border-0 shadow-lg surface-card">
        <div className="card-body p-4 p-md-5">
          <div className="mb-4">
            <div className="metric-chip d-inline-flex mb-3">Inventory control center</div>
            <h2 className="mb-1">Welcome back</h2>
            <p className="text-muted mb-0">Sign in to your inventory workspace with a secure session.</p>
          </div>
          <form onSubmit={onSubmit} className="d-flex flex-column gap-3">
            <div>
              <label className="form-label">Username (email)</label>
              <div className="auth-input-shell">
                <FaEnvelope className="auth-input-icon" aria-hidden="true" />
                <input
                  className={`form-control ${errors.username ? 'is-invalid' : ''}`}
                  value={values.username}
                  autoComplete="username"
                  placeholder="name@example.com"
                  onChange={(e) => setValues((prev) => ({ ...prev, username: e.target.value }))}
                />
              </div>
              {errors.username ? <div className="invalid-feedback">{errors.username}</div> : null}
            </div>
            <div>
              <label className="form-label">Password</label>
              <div className="auth-input-shell auth-password-shell">
                <FaLock className="auth-input-icon" aria-hidden="true" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                  value={values.password}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  onChange={(e) => setValues((prev) => ({ ...prev, password: e.target.value }))}
                />
                <button
                  type="button"
                  className="btn auth-eye-btn"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
              {errors.password ? <div className="invalid-feedback">{errors.password}</div> : null}
            </div>
            <div className="small text-muted auth-helper">Use your registered email and password to continue.</div>
            <button className="btn btn-modern btn-modern-primary" disabled={loading}>{loading ? 'Signing in...' : 'Login'}</button>
          </form>
          <div className="mt-4 text-muted small">
            Need an account? <Link to="/register">Register</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
