import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import logo from '../../assets/logo.png';
import { useAdminAuth } from '../../context/AdminAuthContext';

export function AdminLoginPage() {
  const { login } = useAdminAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      navigate('/admin', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="admin-login">
      <form className="admin-login-card" onSubmit={handleSubmit}>
        <img src={logo} alt="" className="admin-login-logo" />
        <h1 className="admin-login-title">Liams Creations</h1>
        <p className="admin-login-sub">Admin sign in</p>

        {error && <p className="admin-login-error">{error}</p>}

        <label className="admin-field">
          <span className="admin-field-label">Username</span>
          <input
            type="text"
            className="admin-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
            required
          />
        </label>

        <label className="admin-field">
          <span className="admin-field-label">Password</span>
          <input
            type="password"
            className="admin-input"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        <button type="submit" className="admin-login-btn" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
