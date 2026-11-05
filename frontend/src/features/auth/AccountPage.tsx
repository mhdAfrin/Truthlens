import { useState } from "react";
import { ArrowRight, LogOut, UserRound } from "lucide-react";
import { go } from "../../lib/navigation";

import { useAuth } from "./AuthContext";
export function AccountPage() {
  const { user, logout } = useAuth();
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  if (!user) return null;
  async function signOut() {
    setBusy(true);
    setError("");
    try {
      await logout();
      go("login");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-panel account-details">
      <span className="account-symbol">
        <UserRound size={24} />
      </span>
      <span className="eyebrow">PERSONAL WORKSPACE</span>
      <h1>Hello, {user.name}.</h1>
      <p>You’re signed in securely to TruthLens.</p>
      <dl>
        <div>
          <dt>Email address</dt>
          <dd>{user.email}</dd>
        </div>
        <div>
          <dt>Account type</dt>
          <dd>{user.role === "ADMIN" ? "Administrator" : "Member"}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>Active</dd>
        </div>
      </dl>
      <div className="account-buttons">
        <button className="button primary" onClick={() => go("library")}>
          Open verification history <ArrowRight size={16} />
        </button>
        <button className="button quiet" disabled={busy} onClick={signOut}>
          <LogOut size={16} />
          {busy ? "Signing out…" : "Sign out"}
        </button>
      </div>
      {error && (
        <p className="registration-error" role="alert">
          {error}
        </p>
      )}
      <p className="auth-footnote">
        Your submitted reports are saved to your account. Only you can open your personal history.
      </p>
    </div>
  );
}
