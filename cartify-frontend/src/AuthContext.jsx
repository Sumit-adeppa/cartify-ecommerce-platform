import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api } from "./api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);

  const refreshSession = async () => {
    try {
      const data = await api("/api/auth/validate", { method: "POST" });
      if (data?.valid && data.userId != null) {
        const current = {
          userId: data.userId,
          username: data.username || "Customer",
          role: String(data.role || "CUSTOMER").toUpperCase()
        };
        setUser(current);
        return current;
      }
      setUser(null);
      return null;
    } catch {
      setUser(null);
      return null;
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => {
    refreshSession();
  }, []);

  const login = async (username, password) => {
    await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    const session = await refreshSession();
    if (!session) throw new Error("Login succeeded, but the session could not be validated. Check the auth validation endpoint and authToken cookie.");
    return session;
  };

  const register = (data) =>
    api("/api/users/register", {
      method: "POST",
      body: JSON.stringify(data)
    });

  const logout = async () => {
    try {
      await api("/api/auth/logout", { method: "POST" });
    } finally {
      setUser(null);
    }
  };

  const value = useMemo(() => ({ user, checking, login, register, logout, refreshSession }), [user, checking]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
