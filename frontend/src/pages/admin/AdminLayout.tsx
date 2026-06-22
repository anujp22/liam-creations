import { Link, Outlet, useNavigate } from 'react-router-dom';
import logo from '../../assets/logo.png';
import { useAdminAuth } from '../../context/AdminAuthContext';

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
            <span className="admin-brand-sub">Admin</span>
          </span>
        </Link>
        <div className="admin-header-right">
          <Link to="/" className="admin-viewsite">View site ↗</Link>
          {username && <span className="admin-user">{username}</span>}
          <button onClick={handleLogout} className="admin-logout">Log out</button>
        </div>
      </header>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
