import { useEffect, useRef, useState } from "react";
import { ArrowRight, Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";
import { go } from "../../lib/navigation";

import { useAuth } from "./AuthContext";
export function LoginPage() {
  const { user, login } = useAuth();
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [show, setShow] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const first = useRef<HTMLInputElement>(null);
  useEffect(() => {
    first.current?.focus();
  }, []);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    if (!email.trim() || !password) {
      setError("Enter your email and password.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await login(email, password);
      setPassword("");
      go("account");
    } catch (e) {
      setError((e as Error).message);
      setPassword("");
    } finally {
      setBusy(false);
    }
  }
  if (user)
    return (
      <div className="auth-panel">
        <ShieldCheck size={30} />
        <h1>You’re signed in.</h1>
        <p>Welcome back, {user.name}.</p>
        <button className="button primary" onClick={() => go("account")}>
          Go to my account <ArrowRight size={17} />
        </button>
      </div>
    );
  return (
    <div className="auth-layout">
      <aside className="auth-intro">
        <span className="eyebrow">YOUR PERSONAL WORKSPACE</span>
        <h1>
          Welcome back.
          <br />
          Keep your perspective.
        </h1>
        <p>Sign in to access your account and continue exploring the evidence.</p>
        <div>
          <ShieldCheck size={24} />
          <p>
            Your account stays protected. Sign out when you finish, especially on a shared device.
          </p>
        </div>
        <small>New to TruthLens?</small>
        <button className="button quiet" onClick={() => go("register")}>
          Create an account <ArrowRight size={16} />
        </button>
      </aside>
      <section className="auth-form">
        <span className="account-symbol">
          <LockKeyhole size={23} />
        </span>
        <h2>Sign in to TruthLens</h2>
        <p>Use the email and password you registered with.</p>
        <form onSubmit={submit} noValidate aria-busy={busy}>
          <div className="registration-field">
            <label htmlFor="login-email">Email address</label>
            <input
              ref={first}
              id="login-email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={busy}
              required
            />
          </div>
          <div className="registration-field">
            <label htmlFor="login-password">Password</label>
            <div className="password-input">
              <input
                id="login-password"
                name="password"
                type={show ? "text" : "password"}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={busy}
                required
              />
              <button
                type="button"
                className="password-toggle"
                aria-label={show ? "Hide password" : "Show password"}
                aria-pressed={show}
                onClick={() => setShow(!show)}
                disabled={busy}
              >
                {show ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>
          {error && (
            <p className="registration-error" role="alert">
              {error}
            </p>
          )}
          <button className="button primary register-submit" disabled={busy}>
            {busy ? "Signing in…" : "Sign in"}
            <ArrowRight size={17} />
          </button>
        </form>
        <p className="auth-footnote">Your session expires after 30 minutes of inactivity.</p>
        <button className="text-button" onClick={() => go("register")}>
          Don’t have an account? Register
        </button>
      </section>
    </div>
  );
}
