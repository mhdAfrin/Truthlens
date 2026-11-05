import { ArrowUpRight, LogOut, RefreshCw, Download } from "lucide-react";

import { Brand } from "../../components/UI";

import { useAdminWorkspace } from "./useAdminWorkspace";
import { tabs } from "./adminConfig";
import { AdminOverview } from "./AdminOverview";
import { AdminRecords } from "./AdminRecords";
import { AdminSubmissionDetail } from "./AdminSubmissionDetail";
import { AdminNewsDesk } from "./AdminNewsDesk";
import { AdminActivity } from "./AdminActivity";
import { AdminSystem } from "./AdminSystem";
import { AdminLogin } from "./AdminLogin";
export function AdminWorkspace() {
  const workspace = useAdminWorkspace();
  const {
    user,
    loading,
    tab,
    data,
    error,
    busy,
    detail,
    notice,
    signOut,
    navigate,
    exportData,
    setRevision,
  } = workspace;
  if (loading)
    return (
      <div className="admin-loading" role="status">
        Checking administrator access…
      </div>
    );
  if (user?.role !== "ADMIN") return <AdminLogin state={workspace} />;
  return (
    <div className="admin-app">
      <aside className="admin-sidebar">
        <a href="/" aria-label="TruthLens public website">
          <Brand />
        </a>
        <span className="admin-eyebrow">CONTROL CENTRE</span>
        <nav aria-label="Admin navigation">
          {tabs.map(([id, label, Icon]) => (
            <button
              key={id}
              className={tab === id ? "active" : ""}
              aria-current={tab === id ? "page" : undefined}
              onClick={() => navigate(id)}
            >
              <Icon size={19} />
              {label}
            </button>
          ))}
        </nav>
        <div className="admin-identity">
          <strong>{user.name}</strong>
          <small>{user.email}</small>
          <button onClick={signOut} disabled={busy}>
            <LogOut size={16} /> Sign out
          </button>
        </div>
      </aside>
      <div className="admin-main">
        <header>
          <span>
            Operations <span>/</span> {tabs.find((t) => t[0] === tab)?.[1]}
          </span>
          <a href="/">
            Open website <ArrowUpRight size={15} />
          </a>
        </header>
        <main>
          <div className="admin-page-heading">
            <div>
              <p className="admin-eyebrow">YOUR WORKSPACE, AT A GLANCE</p>
              <h1>{tabs.find((t) => t[0] === tab)?.[1]}</h1>
              <p>
                {
                  {
                    overview: "A focused view of the platform and what needs attention.",
                    users: "Manage account access without exposing passwords.",
                    submissions: "Follow intake activity and inspect requests when needed.",
                    news: "Curate useful stories and independent fact-checks for the public home page.",
                    audit: "A record of administrative changes and private submission access.",
                    system: "Database health, schema versions and service configuration.",
                  }[tab]
                }
              </p>
            </div>
            <div className="admin-actions">
              <button onClick={() => setRevision((n) => n + 1)} aria-label="Refresh admin data">
                <RefreshCw size={16} />
                Refresh
              </button>
              {data && (
                <button onClick={exportData}>
                  <Download size={16} />
                  Export view
                </button>
              )}
            </div>
          </div>
          {error && (
            <p role="alert" className="admin-error">
              {error}
            </p>
          )}
          {notice && (
            <p role="status" className="admin-success">
              {notice}
            </p>
          )}
          {!data && !error && (
            <p role="status" className="admin-panel">
              Loading workspace…
            </p>
          )}
          {data && tab === "overview" && <AdminOverview state={workspace} />}
          {data && (tab === "users" || tab === "submissions") && <AdminRecords state={workspace} />}
          {detail && tab === "submissions" && <AdminSubmissionDetail state={workspace} />}
          {data && tab === "news" && <AdminNewsDesk state={workspace} />}
          {data && tab === "audit" && <AdminActivity state={workspace} />}
          {data && tab === "system" && <AdminSystem state={workspace} />}
        </main>
        <footer>TruthLens operations · Access is checked on every request.</footer>
      </div>
    </div>
  );
}
