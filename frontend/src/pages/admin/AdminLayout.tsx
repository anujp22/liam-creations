import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import logo from '../../assets/logo.png';
import { useAdminAuth } from '../../context/AdminAuthContext';

const TABS = [
  { to: '/admin', label: 'Summary', end: true },
  { to: '/admin/products', label: 'Products', end: false },
  { to: '/admin/inventory', label: 'Inventory', end: false },
  { to: '/admin/deleted', label: 'Deleted', end: false },
];

export function AdminLayout() {
  const { username, logout } = useAdminAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/admin/login', { replace: true });
  };

  return (
    <div className="admin">
      <header className="admin-header">
        <Link to="/admin" className="admin-brand">
          <img src={logo} alt="" className="admin-brand-logo" />
          <span className="admin-brand-text">
            <span className="admin-brand-name">Liams Creations</span>
            <span className="admin-brand-sub">Admin Panel</span>
          </span>
        </Link>
        <div className="admin-header-right">
          <a href="/" target="_blank" rel="noopener noreferrer" className="admin-viewsite">View store ↗</a>
          {username && <span className="admin-user">{username}</span>}
          <button onClick={handleLogout} className="admin-logout">Log out</button>
        </div>
      </header>

      <nav className="admin-tabs">
        {TABS.map((t) => (
          <NavLink
            key={t.to}
            to={t.to}
            end={t.end}
            className={({ isActive }) => `admin-tab${isActive ? ' admin-tab--active' : ''}`}
          >
            {t.label}
          </NavLink>
        ))}
        <span className="admin-tab admin-tab--disabled" title="Coming soon">Sales · soon</span>
      </nav>

      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
