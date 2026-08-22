import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import {
  fetchCurrentAdmin,
  login as loginRequest,
  logout as logoutRequest,
  setUnauthorizedHandler,
} from '../api/admin';

interface AdminAuthContextValue {
  /** True only once the server has confirmed a session. */
  isAuthenticated: boolean;
  /** True until the initial session check finishes — routes must wait, not redirect. */
  isChecking: boolean;
  username: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null);
  // The session lives in an HttpOnly cookie this code cannot read, so unlike the old
  // sessionStorage token there is nothing to inspect synchronously. The server is the
  // only thing that knows, which is why every load starts in the checking state.
  const [isChecking, setIsChecking] = useState(true);

  const clearLocalState = () => setUsername(null);

  useEffect(() => {
    // Let the API layer trigger logout on any 401 — a session that expired or was
    // revoked server-side shows up as one on the next request.
    setUnauthorizedHandler(clearLocalState);
    return () => setUnauthorizedHandler(null);
  }, []);

  useEffect(() => {
    let cancelled = false;
    // Restores the session across a refresh, and doubles as the call that seeds the
    // CSRF cookie the login form needs — it runs logged-out too, and 401 still carries
    // the token.
    fetchCurrentAdmin()
      .then((name) => {
        if (!cancelled) setUsername(name);
      })
      .catch(() => {
        if (!cancelled) setUsername(null);
      })
      .finally(() => {
        if (!cancelled) setIsChecking(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = async (user: string, password: string) => {
    const verifiedName = await loginRequest(user, password); // throws on bad creds
    setUsername(verifiedName);
  };

  const logout = async () => {
    // Clear locally first so the UI never sits on an admin screen waiting for the
    // network; the server-side invalidation is what actually revokes the session.
    clearLocalState();
    await logoutRequest();
  };

  return (
    <AdminAuthContext.Provider
      value={{ isAuthenticated: username !== null, isChecking, username, login, logout }}
    >
      {children}
    </AdminAuthContext.Provider>
  );
}

// Hook colocated with its provider by design; the fast-refresh rule only cares
// about mixed exports, which is harmless here.
// eslint-disable-next-line react-refresh/only-export-components
export function useAdminAuth() {
  const ctx = useContext(AdminAuthContext);
  if (!ctx) throw new Error('useAdminAuth must be used within AdminAuthProvider');
  return ctx;
}
