import type { SystemData } from "./adminTypes";
import { type AdminWorkspaceState } from "./useAdminWorkspace";

export function AdminSystem({ state }: { state: AdminWorkspaceState }) {
  const data = state.data as SystemData;
  const { password } = state;
  return (
    <>
      <div className="admin-two">
        <section className="admin-panel">
          <h2>Service health</h2>
          <dl>
            {[
              ["Database", data.database],
              ["Connection", data.databaseStatus],
              ["Sessions", data.sessions],
              ["Password storage", data.passwordStorage],
              ["Verification engine", data.verificationEngine],
            ].map(([k, v]) => (
              <div key={k}>
                <dt>{k}</dt>
                <dd>{v}</dd>
              </div>
            ))}
          </dl>
        </section>
        <section className="admin-panel">
          <h2>Data inventory</h2>
          <p>Record counts only. Database credentials and password hashes are never returned.</p>
          <dl>
            {data.tables.map((t) => (
              <div key={t.name}>
                <dt>{t.name}</dt>
                <dd>{t.rows} rows</dd>
              </div>
            ))}
          </dl>
        </section>
      </div>
      <section className="admin-panel">
        <h2>Schema migrations</h2>
        {data.migrations.map((m, i) => (
          <div className="admin-migration" key={i}>
            <strong>V{m.version || m.VERSION}</strong>
            <span>{m.description || m.DESCRIPTION}</span>
            <span className="admin-pill good">Applied</span>
          </div>
        ))}
      </section>
    </>
  );
}
