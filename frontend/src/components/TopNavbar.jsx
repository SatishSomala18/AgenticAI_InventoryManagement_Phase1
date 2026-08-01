import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaBoxesStacked, FaRightFromBracket, FaBell, FaSun, FaMoon } from 'react-icons/fa6';
import { useAuth } from '../hooks/useAuth';
import { useAuthorization } from '../hooks/useAuthorization';
import { humanizeEnum } from '../utils/formatters';
import { getAlerts } from '../services/inventoryService';

export default function TopNavbar() {
  const { fullName, role, logout } = useAuth();
  const { canRoute } = useAuthorization();

  const [dark, setDark] = useState(() => {
    const saved = localStorage.getItem('inv-dark-mode') === 'true';
    if (saved) document.body.classList.add('dark-mode');
    return saved;
  });

  const [alertCount, setAlertCount] = useState(0);

  useEffect(() => {
    document.body.classList.toggle('dark-mode', dark);
    localStorage.setItem('inv-dark-mode', String(dark));
  }, [dark]);

  // Fetch open alert count for roles that can access alerts
  useEffect(() => {
    if (!canRoute('alerts')) return;
    let cancelled = false;
    getAlerts()
      .then((alerts) => {
        if (!cancelled) setAlertCount(alerts.filter((a) => a.status === 'OPEN').length);
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [role]); // re-fetch only on role change (login/logout)

  const displayName = fullName || humanizeEnum(role);

  return (
    <header className="navbar navbar-expand-lg app-navbar px-3">
      <div className="container-fluid p-0">
        <span className="navbar-brand d-flex align-items-center gap-2 text-light fw-semibold m-0">
          <FaBoxesStacked />
          Inventory Management
        </span>

        <div className="d-flex align-items-center gap-3 ms-auto">
         

          {/* Alerts bell — visible only for roles that can access /alerts */}
          {canRoute('alerts') && (
            <Link to="/alerts" className="navbar-icon-btn position-relative" title="Open alerts" aria-label={`${alertCount} open alerts`}>
              <FaBell />
              {alertCount > 0 && (
                <span className="navbar-alert-badge">{alertCount > 99 ? '99+' : alertCount}</span>
              )}
            </Link>
          )}

          {/* User info — shows full name, not email */}
          <div className="text-end small text-light">
            <div className="fw-semibold">{displayName}</div>
            <div className="opacity-75">{humanizeEnum(role)}</div>
          </div>

          <button className="btn btn-sm btn-outline-light btn-modern btn-modern-secondary" onClick={logout}>
            <FaRightFromBracket className="me-2" />
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
