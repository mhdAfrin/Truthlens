import { ArrowUpRight, ShieldCheck, LockKeyhole } from "lucide-react";

import { Brand } from "../../components/UI";

import { type AdminWorkspaceState } from "./useAdminWorkspace";

export function AdminLogin({ state }: { state: AdminWorkspaceState }) {
  const { user, error, busy, email, password, setEmail, setPassword, signIn } = state;
  return (
    <div className="admin-login">
      <section className="admin-login-story">
        <Brand />
        <span className="admin-eyebrow">TRUTHLENS OPERATIONS</span>
        <h1>
          A clearer view.
          <br />
          Behind the scenes.
        </h1>
        <p>Manage the people, submissions and editorial work that keep TruthLens moving.</p>
        <div>
          <ShieldCheck size={24} />
          <span>
            Restricted workspace
            <br />
            <small>Administrator accounts only</small>
          </span>
        </div>
        <a href="/">
          Back to TruthLens <ArrowUpRight size={16} />
        </a>
      </section>
      <section className="admin-login-form">
        <div>
          <span className="admin-lock">
            <LockKeyhole />
          </span>
          <p className="admin-eyebrow">WELCOME BACK</p>
          <h2>Administrator sign in</h2>
          <p>Use your private administrator credentials.</p>
          {user && (
            <p role="alert" className="admin-error">
              This account does not have administrator access. Sign in below with your administrator
              credentials.
            </p>
          )}
          {error && (
            <p role="alert" className="admin-error">
              {error}
            </p>
          )}
          <form onSubmit={signIn}>
            <label htmlFor="admin-email">Admin email</label>
            <input
              id="admin-email"
              type="email"
              required
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <label htmlFor="admin-password">Password</label>
            <input
              id="admin-password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button className="primary" disabled={busy}>
              {busy ? "Signing in…" : "Sign in securely"}
              <ArrowUpRight size={17} />
            </button>
          </form>
          <small>
            Access is controlled by your account role. Sessions expire after 30 minutes of
            inactivity.
          </small>
        </div>
      </section>
    </div>
  );
}
