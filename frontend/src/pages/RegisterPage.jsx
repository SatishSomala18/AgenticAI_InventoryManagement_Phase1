import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { FaEye, FaEyeSlash, FaEnvelope, FaLock, FaUser } from 'react-icons/fa6';
import { useAuth } from '../hooks/useAuth';
import { ROLES } from '../utils/constants';
import { parseApiError } from '../utils/apiError';
import { validateRegister } from '../utils/validation';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [loading, setLoading] = useState(false);
  const [values, setValues] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    role: ROLES.WAREHOUSE_STAFF,
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const onSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateRegister(values);
    if (values.password !== values.confirmPassword) {
      nextErrors.confirmPassword = 'Passwords do not match';
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    try {
      setLoading(true);
      await register({
        email: values.email,
        password: values.password,
        fullName: values.fullName,
        role: values.role,
      });
      toast.success('Registration successful. You can now login.');
      navigate('/login');
    } catch (error) {
      toast.error(parseApiError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card auth-card-wide card border-0 shadow-lg surface-card">
        <div className="card-body p-3 p-md-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
            <div>
              <div className="metric-chip d-inline-flex mb-2">Team onboarding</div>
              <h2 className="mb-1">Create account</h2>
              <p className="text-muted mb-0">Create a secure account with role-based access.</p>
            </div>
          </div>
          <form onSubmit={onSubmit} className="row g-2 g-md-3">
            <div className="col-md-6">
              <label className="form-label mb-1">Email</label>
              <div className="auth-input-shell compact-auth-input">
                <FaEnvelope className="auth-input-icon" aria-hidden="true" />
                <input
                  className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                  value={values.email}
                  autoComplete="email"
                  placeholder="name@example.com"
                  onChange={(e) => setValues((prev) => ({ ...prev, email: e.target.value }))}
                />
              </div>
              {errors.email ? <div className="invalid-feedback">{errors.email}</div> : null}
            </div>
            <div className="col-md-6">
              <label className="form-label mb-1">Full Name</label>
              <div className="auth-input-shell compact-auth-input">
                <FaUser className="auth-input-icon" aria-hidden="true" />
                <input
                  className={`form-control ${errors.fullName ? 'is-invalid' : ''}`}
                  value={values.fullName}
                  autoComplete="name"
                  placeholder="Your full name"
                  onChange={(e) => setValues((prev) => ({ ...prev, fullName: e.target.value }))}
                />
              </div>
              {errors.fullName ? <div className="invalid-feedback">{errors.fullName}</div> : null}
            </div>
            <div className="col-md-6">
              <label className="form-label mb-1">Password</label>
              <div className="auth-input-shell auth-password-shell compact-auth-input">
                <FaLock className="auth-input-icon" aria-hidden="true" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                  value={values.password}
                  autoComplete="new-password"
                  placeholder="At least 6 characters"
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
            <div className="col-md-6">
              <label className="form-label mb-1">Confirm Password</label>
              <div className="auth-input-shell auth-password-shell compact-auth-input">
                <FaLock className="auth-input-icon" aria-hidden="true" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`}
                  value={values.confirmPassword}
                  autoComplete="new-password"
                  placeholder="Re-enter password"
                  onChange={(e) => setValues((prev) => ({ ...prev, confirmPassword: e.target.value }))}
                />
                <button
                  type="button"
                  className="btn auth-eye-btn"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                >
                  {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
              {errors.confirmPassword ? <div className="invalid-feedback d-block">{errors.confirmPassword}</div> : null}
            </div>
            <div className="col-md-6">
              <label className="form-label mb-1">Role</label>
              <select
                className="form-select"
                value={values.role}
                onChange={(e) => setValues((prev) => ({ ...prev, role: e.target.value }))}
              >
                {Object.values(ROLES).map((role) => <option key={role} value={role}>{role}</option>)}
              </select>
            </div>
            <div className="col-md-6 d-flex align-items-end">
              <div className="small text-muted auth-helper mb-1">Choose role carefully. It controls page access after login.</div>
            </div>
            <div className="col-12 pt-1">
              <button className="btn btn-modern btn-modern-primary w-100" disabled={loading}>{loading ? 'Submitting...' : 'Register'}</button>
            </div>
          </form>
          <div className="mt-3 text-muted small text-center">
            Already have an account? <Link to="/login">Login</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
