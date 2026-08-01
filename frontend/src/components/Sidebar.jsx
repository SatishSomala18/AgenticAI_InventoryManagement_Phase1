import { NavLink } from 'react-router-dom';
import { FaChartLine, FaBoxesStacked, FaTruckField, FaClipboardList, FaWarehouse, FaTriangleExclamation } from 'react-icons/fa6';
import { useAuth } from '../hooks/useAuth';
import { ROLES } from '../utils/constants';
import { humanizeEnum } from '../utils/formatters';

const navConfig = [
  { to: '/dashboard', label: 'Dashboard', icon: FaChartLine, roles: [ROLES.STORE_MANAGER] },
  { to: '/products', label: 'Products', icon: FaBoxesStacked, roles: Object.values(ROLES) },
  { to: '/suppliers', label: 'Suppliers', icon: FaTruckField, roles: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER, ROLES.INVENTORY_ANALYST] },
  { to: '/orders', label: 'Purchase Orders', icon: FaClipboardList, roles: [ROLES.STORE_MANAGER, ROLES.PROCUREMENT_OFFICER, ROLES.INVENTORY_ANALYST, ROLES.WAREHOUSE_STAFF] },
  { to: '/stock', label: 'Stock', icon: FaWarehouse, roles: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST, ROLES.WAREHOUSE_STAFF] },
  { to: '/alerts', label: 'Alerts', icon: FaTriangleExclamation, roles: [ROLES.STORE_MANAGER, ROLES.INVENTORY_ANALYST] },
];

export default function Sidebar() {
  const { role } = useAuth();

  return (
    <aside className="app-sidebar p-3" aria-label="Main navigation sidebar">
      <div className="mb-3 px-2">
        <div className="small text-uppercase text-white-50 fw-semibold letter-space-wide">Workspace</div>
        <div className="text-white fw-semibold">{humanizeEnum(role) || 'Guest'}</div>
      </div>
      <nav className="nav flex-column gap-1" aria-label="Primary">
        {navConfig
          .filter((item) => item.roles.includes(role))
          .map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-link side-link ${isActive ? 'active' : ''}`}
              >
                <Icon />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
      </nav>
    </aside>
  );
}
