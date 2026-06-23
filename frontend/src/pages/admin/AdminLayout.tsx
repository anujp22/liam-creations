import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import logo from '../../assets/logo.png';
import { useAdminAuth } from '../../context/AdminAuthContext';

const NAV = [
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
      <aside className="admin-sidebar">
        {/* Logo links to the storefront in the same tab; the admin stays logged in
            and returns via the "Back to admin" bar on the store. */}
        <Link to="/" className="admin-brand" title="Go to store">
          <img src={logo} alt="" className="admin-brand-logo" />
          <span className="admin-brand-text">
            <span className="admin-brand-name">Liams Creations</span>
            <span className="admin-brand-badge">Admin</span>
          </span>
        </Link>

        <nav className="admin-nav">
          {NAV.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) => `admin-nav-item${isActive ? ' admin-nav-item--active' : ''}`}
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="admin-content">
        <header className="admin-topbar">
          <span className="admin-topbar-user">
            Signed in as <strong className="admin-topbar-name">{username ?? 'admin'}</strong>
          </span>
          <button onClick={handleLogout} className="admin-logout">Log out</button>
        </header>
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
