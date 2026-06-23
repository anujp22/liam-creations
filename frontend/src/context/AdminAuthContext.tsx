import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import {
  clearToken,
  encodeBasicToken,
  getStoredToken,
  setUnauthorizedHandler,
  storeToken,
  verifyCredentials,
} from '../api/admin';

interface AdminAuthContextValue {
  isAuthenticated: boolean;
  username: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null);
  const hasToken = getStoredToken() !== null;
  // Trust an existing sessionStorage token optimistically; any stale token is
  // caught on the first request (401 -> logout).
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(hasToken);

  const logout = () => {
    clearToken();
    setUsername(null);
    setIsAuthenticated(false);
  };

  // Let the API layer trigger logout on any 401.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, []);

  const login = async (user: string, password: string) => {
    const token = encodeBasicToken(user, password);
    const verifiedName = await verifyCredentials(token); // throws on bad creds
    storeToken(token);
    setUsername(verifiedName);
    setIsAuthenticated(true);
  };

  return (
    <AdminAuthContext.Provider value={{ isAuthenticated, username, login, logout }}>
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
