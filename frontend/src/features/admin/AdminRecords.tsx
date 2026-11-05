import type { RecordsData, AdminUser, AdminSubmission } from "./adminTypes";
import { Search } from "lucide-react";

import { type AdminWorkspaceState } from "./useAdminWorkspace";
import { date } from "./adminHelpers";
import { Pager } from "./AdminPagination";
export function AdminRecords({ state }: { state: AdminWorkspaceState }) {
  const data = state.data as RecordsData;
  const { tab, busy, query, page, status, setQuery, setPage, setStatus, toggle, inspect } = state;
  return (
    <section className="admin-panel">
      <div className="admin-toolbar">
        {tab === "users" ? (
          <label>
            <Search size={17} />
            <input
              placeholder="Search name or email"
              aria-label="Search users"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(0);
              }}
            />
          </label>
        ) : (
          <select
            aria-label="Submission status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            <option>PROCESSING</option>
            <option>AWAITING_VERIFICATION</option>
            <option>FAILED</option>
            <option>COMPLETED</option>
          </select>
        )}
        <span>{data.total} records</span>
      </div>
      <div className="admin-table-scroll">
        <table>
          <thead>
            <tr>
              {(tab === "users"
                ? ["Person", "Role", "Status", "Joined", "Access"]
                : ["Submission", "Type / owner", "Status", "Submitted", "Details"]
              ).map((h) => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => {
              if (tab === "users") {
                const u = item as AdminUser;
                return (
                  <tr key={u.id}>
                    <td>
                      <strong>{u.name}</strong>
                      <small>{u.email}</small>
                    </td>
                    <td>{u.role}</td>
                    <td>
                      <span className={`admin-pill ${u.active ? "good" : ""}`}>
                        {u.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td>{date(u.createdAt)}</td>
                    <td>
                      {u.role === "ADMIN" ? (
                        <small>Protected administrator</small>
                      ) : (
                        <button disabled={busy} onClick={() => toggle(u)}>
                          {u.active ? "Deactivate" : "Activate"}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              }
              const u = item as AdminSubmission;
              return (
                <tr key={u.id}>
                  <td>
                    <strong>{u.title}</strong>
                    <small>{u.id}</small>
                  </td>
                  <td>
                    {u.inputType}
                    <small>{u.ownership}</small>
                  </td>
                  <td>
                    <span className="admin-pill">
                      {u.status.replaceAll("_", " ").toLowerCase()}
                    </span>
                  </td>
                  <td>{date(u.createdAt)}</td>
                  <td>
                    <button onClick={() => inspect(u.id)}>Inspect</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!data.items.length && <p className="admin-empty">No records match this view.</p>}
      </div>
      <Pager page={page} total={data.total} size={20} setPage={setPage} />
    </section>
  );
}
