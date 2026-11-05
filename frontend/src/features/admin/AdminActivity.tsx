import type { AuditData } from "./adminTypes";
import { type AdminWorkspaceState } from "./useAdminWorkspace";
import { date } from "./adminHelpers";
import { Pager } from "./AdminPagination";
export function AdminActivity({ state }: { state: AdminWorkspaceState }) {
  const data = state.data as AuditData;
  const { page, setPage } = state;
  return (
    <section className="admin-panel">
      <div className="admin-table-scroll">
        <table>
          <thead>
            <tr>
              <th>Action</th>
              <th>Administrator</th>
              <th>Target</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((raw, i) => {
              const a = Object.fromEntries(
                Object.entries(raw).map(([k, v]) => [k.toLowerCase(), v]),
              ) as Record<string, string>;
              return (
                <tr key={i}>
                  <td>{a.action.replaceAll("_", " ").toLowerCase()}</td>
                  <td>{a.actor}</td>
                  <td>{a.target}</td>
                  <td>{date(a.created_at)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!data.items.length && (
          <p className="admin-empty">Administrative changes will appear here.</p>
        )}
      </div>
      <Pager page={page} total={data.total} size={30} setPage={setPage} />
    </section>
  );
}
