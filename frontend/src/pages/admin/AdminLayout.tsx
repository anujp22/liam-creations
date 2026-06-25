import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import logo from '../../assets/logo.png';
import { useAdminAuth } from '../../context/AdminAuthContext';
import { fetchPendingReviewCount } from '../../api/admin';

const NAV = [
  { to: '/admin', label: 'Summary', end: true },
  { to: '/admin/products', label: 'Products', end: false },
  { to: '/admin/on-sale', label: 'On Sale', end: false },
  { to: '/admin/reviews', label: 'Reviews', end: false },
  { to: '/admin/inventory', label: 'Inventory', end: false },
  { to: '/admin/deleted', label: 'Deleted', end: false },
];

export function AdminLayout() {
  const { username, logout } = useAdminAuth();
  const navigate = useNavigate();
  const { data: pendingReviews = 0 } = useQuery({
    queryKey: ['admin-pending-reviews'],
    queryFn: fetchPendingReviewCount,
    staleTime: 30_000,
  });

  const handleLogout = () => {
    logout();
    navigate('/admin/login', { replace: true });
  };

  return (
    <div className="admin">
      <aside className="admin-sidebar">
        {/* Logo links to the storefront in the same tab; the admin stays logged
            in and returns via the Admin link in the store header. */}
        <Link to="/" className="admin-brand" title="Go to store">
          <img src={logo} alt="" className="admin-brand-logo" />
          <span className="admin-brand-name">Liams Creations</span>
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
              {n.to === '/admin/reviews' && pendingReviews > 0 && (
                <span className="admin-nav-badge">{pendingReviews}</span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="admin-sidebar-foot">
          <span className="admin-sidebar-user">
            Signed in as <strong className="admin-sidebar-name">{username ?? 'admin'}</strong>
          </span>
          <button onClick={handleLogout} className="admin-logout">Log out</button>
        </div>
      </aside>

      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
