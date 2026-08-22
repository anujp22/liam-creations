import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAdminAuth } from '../context/AdminAuthContext';

/** Route guard: redirects to the login page when not authenticated. */
export function RequireAdmin() {
  const { isAuthenticated, isChecking } = useAdminAuth();
  const location = useLocation();

  // Whether there is a session is now a question only the server can answer, so the
  // first render of any admin URL happens before the answer arrives. Redirecting here
  // would bounce a logged-in owner to the login screen on every refresh.
  if (isChecking) {
    return <p className="admin-placeholder">Loading…</p>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}
