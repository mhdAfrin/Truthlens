import { useAuth } from "../features/auth/AuthContext";
import { useEffect, useRef, useState } from "react";
import {
  ScanLine,
  Plus,
  History,
  FileCheck2,
  Library,
  BookOpen,
  ChevronRight,
  LockKeyhole,
  Menu,
  HelpCircle,
  X,
  UserPlus,
} from "lucide-react";
import { Brand } from "./UI";
import { go, type Page } from "../lib/navigation";
const routes = [
  ["verify", "New verification", Plus],
  ["library", "Verification history", History],
  ["claims", "Claims registry", FileCheck2],
  ["sources", "Source records", Library],
  ["method", "Methodology", BookOpen],
  ["help", "Help & guidance", HelpCircle],
] as const;
export function Shell({ page, children }: { page: Page; children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const [signingOut, setSigningOut] = useState(false),
    [logoutError, setLogoutError] = useState("");
  async function signOut() {
    setSigningOut(true);
    setLogoutError("");
    try {
      await logout();
      go("login");
    } catch (e) {
      setLogoutError((e as Error).message);
    } finally {
      setSigningOut(false);
    }
  }
  const [mobile, setMobile] = useState(false);
  const drawer = useRef<HTMLElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  useEffect(() => setMobile(false), [page]);
  useEffect(() => {
    if (!mobile) return;
    drawer.current?.querySelector<HTMLButtonElement>("button")?.focus();
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMobile(false);
        trigger.current?.focus();
      }
    };
    document.addEventListener("keydown", close);
    return () => document.removeEventListener("keydown", close);
  }, [mobile]);
  function navigate(p: Page) {
    go(p);
    setMobile(false);
  }
  const title =
    page === "login"
      ? "Sign in"
      : page === "account"
        ? "My account"
        : page === "register"
          ? "Create account"
          : page === "report"
            ? "Verification report"
            : routes.find((r) => r[0] === page)?.[1];
  return (
    <div className="app original-ui">
      <a
        className="skip-link"
        href="#main-content"
        onClick={(e) => {
          e.preventDefault();
          document.getElementById("main-content")?.focus();
        }}
      >
        Skip to content
      </a>
      <button
        className={mobile ? "drawer-scrim visible" : "drawer-scrim"}
        aria-label="Close navigation"
        onClick={() => setMobile(false)}
      />
      <aside
        ref={drawer}
        id="workspace-navigation"
        className={mobile ? "sidebar expanded" : "sidebar"}
      >
        <button
          className="original-brand-button"
          aria-label="TruthLens home"
          onClick={() => navigate("verify")}
        >
          <Brand />
        </button>
        <div className="workspace-label">INVESTIGATION WORKSPACE</div>
        <nav aria-label="Main navigation">
          {routes.map(([id, label, Icon]) => (
            <button
              key={id}
              className={page === id ? "nav active" : "nav"}
              aria-current={page === id ? "page" : undefined}
              onClick={() => navigate(id)}
            >
              <Icon size={19} />
              {label}
              {id === "verify" && <span className="key">N</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <button
            className="profile original-profile"
            onClick={() => navigate(user ? "account" : "login")}
          >
            <span>TL</span>
            <div>
              {user ? user.name : "Personal workspace"}
              <small>{user ? "My account" : "Sign in to your account"}</small>
            </div>
            <UserPlus size={16} />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <button
            className="mobile-brand"
            aria-label="TruthLens home"
            onClick={() => navigate("verify")}
          >
            <ScanLine size={24} />
            TruthLens<span>.</span>
          </button>
          <div className="breadcrumb">
            Workspace <ChevronRight size={14} />
            <strong>{title}</strong>
          </div>
          <div className="top-right">
            {user ? (
              <>
                <button className="account-nav-link" onClick={() => navigate("account")}>
                  My account
                </button>
                <button className="primary account-action" disabled={signingOut} onClick={signOut}>
                  {signingOut ? "Signing out…" : "Sign out"}
                </button>
              </>
            ) : (
              <>
                <button className="account-nav-link" onClick={() => navigate("login")}>
                  Sign in
                </button>
                <button className="primary account-action" onClick={() => navigate("register")}>
                  <UserPlus size={15} />
                  Create account
                </button>
              </>
            )}
          </div>
        </header>
        <main id="main-content" tabIndex={-1}>
          {logoutError && (
            <p className="registration-error" role="alert">
              {logoutError} Your sign-out was not confirmed.
            </p>
          )}
          <div className={page === "verify" ? "original-home" : "enhanced-page"}>{children}</div>
          <footer>
            <span>
              <LockKeyhole size={13} /> Evidence, in perspective.
            </span>
            <span>Transparent methods. Your judgment.</span>
            <button onClick={() => navigate("help")}>About TruthLens</button>
          </footer>
        </main>
      </div>
      <nav className="bottom-nav" aria-label="Mobile navigation">
        {(
          [
            ["verify", "Verify", ScanLine],
            ["library", "History", History],
            ["claims", "Claims", FileCheck2],
          ] as const
        ).map(([id, label, Icon]) => (
          <button
            key={id}
            className={page === id ? "selected" : ""}
            aria-current={page === id ? "page" : undefined}
            onClick={() => navigate(id)}
          >
            <Icon size={21} />
            <span>{label}</span>
          </button>
        ))}
        <button
          ref={trigger}
          className={mobile ? "selected" : ""}
          aria-label="More pages"
          aria-expanded={mobile}
          aria-controls="workspace-navigation"
          onClick={() => setMobile(!mobile)}
        >
          {mobile ? <X size={21} /> : <Menu size={21} />}
          <span>More</span>
        </button>
      </nav>
    </div>
  );
}
