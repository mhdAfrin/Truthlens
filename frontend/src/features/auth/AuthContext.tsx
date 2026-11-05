import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { api, ApiError } from "../../api/client";
import { postAuth } from "../../api/authApi";
import type { Account } from "../../types/account";
type Auth = {
  user: Account | null;
  loading: boolean;
  error: string;
  refresh: () => Promise<Account | null>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};
const Context = createContext<Auth | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Account | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  const version = useRef(0);
  async function refresh() {
    const current = ++version.current;
    try {
      const account = (await api("/api/auth/me")) as Account;
      if (current === version.current) {
        setUser(account);
        setError("");
      }
      return account;
    } catch (e) {
      if (current === version.current) {
        setUser(null);
        setError(e instanceof ApiError && e.status === 401 ? "" : (e as Error).message);
      }
      return null;
    } finally {
      if (current === version.current) setLoading(false);
    }
  }
  useEffect(() => {
    void refresh();
    const check = () => {
      if (document.visibilityState === "visible") void refresh();
    };
    window.addEventListener("focus", check);
    document.addEventListener("visibilitychange", check);
    window.addEventListener("hashchange", check);
    return () => {
      window.removeEventListener("focus", check);
      document.removeEventListener("visibilitychange", check);
      window.removeEventListener("hashchange", check);
      version.current++;
    };
  }, []);
  async function login(email: string, password: string) {
    await postAuth("/api/auth/login", new URLSearchParams({ email: email.trim(), password }));
    const account = await refresh();
    if (!account) throw new Error("Your sign-in could not be confirmed. Please try again.");
  }
  async function logout() {
    await postAuth("/api/auth/logout");
    version.current++;
    setUser(null);
    setError("");
    setLoading(false);
  }
  return (
    <Context.Provider value={{ user, loading, error, refresh, login, logout }}>
      {children}
    </Context.Provider>
  );
}
export function useAuth() {
  const auth = useContext(Context);
  if (!auth) throw Error("AuthProvider is required");
  return auth;
}
