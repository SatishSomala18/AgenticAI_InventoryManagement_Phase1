import { createContext, useEffect, useMemo, useState } from 'react';
import { login as loginApi, register as registerApi } from '../services/authService';
import { TOKEN_STORAGE_KEY } from '../utils/constants';
import { getHomeRouteByRole } from '../utils/accessControl';

export const AuthContext = createContext(null);

function isSessionExpired(session) {
  if (!session?.loggedAt || !session?.expiresInMs) {
    return true;
  }
  return Date.now() >= Number(session.loggedAt) + Number(session.expiresInMs);
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    const raw = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (isSessionExpired(parsed)) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          setSession(null);
        } else {
          setSession(parsed);
        }
      } catch {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    }
    setAuthReady(true);
  }, []);

  const login = async (payload) => {
    const data = await loginApi(payload);
    const nextSession = {
      accessToken: data.accessToken,
      tokenType: data.tokenType,
      expiresInMs: data.expiresInMs,
      username: data.username,
      fullName: data.fullName || null,
      role: data.role,
      loggedAt: Date.now(),
    };
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
    return data;
  };

  const register = async (payload) => registerApi(payload);

  const logout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setSession(null);
  };

  const isAuthenticated = Boolean(session?.accessToken);
  const role = session?.role || null;
  const homeRoute = getHomeRouteByRole(role);
  const hasRole = (expectedRole) => role === expectedRole;
  const hasAnyRole = (allowedRoles = []) => allowedRoles.includes(role);

  const value = useMemo(
    () => ({
      session,
      authReady,
      isAuthenticated,
      role,
      username: session?.username || null,
      fullName: session?.fullName || null,
      homeRoute,
      hasRole,
      hasAnyRole,
      login,
      register,
      logout,
    }),
    [session, authReady, isAuthenticated, role, homeRoute]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
