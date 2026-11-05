import type { OverviewData } from "./adminTypes";
import { ArrowUpRight } from "lucide-react";

import { type AdminWorkspaceState } from "./useAdminWorkspace";
import { type Tab } from "./adminConfig";

export function AdminOverview({ state }: { state: AdminWorkspaceState }) {
  const data = state.data as OverviewData;
  const { navigate } = state;
  return (
    <>
      <div className="admin-stats">
        {[
          ["Registered users", data.users],
          ["Active accounts", data.activeUsers],
          ["Submissions", data.submissions],
          ["Published stories", data.publishedNews],
        ].map(([label, value]) => (
          <section key={label} className="admin-panel">
            <span>{label}</span>
            <strong>{value}</strong>
            <small>Live database count</small>
          </section>
        ))}
      </div>
      <div className="admin-two">
        <section className="admin-panel">
          <h2>Verification pipeline</h2>
          <div className="admin-pipeline">
            <span>
              {data.processing}
              <small>Preparing text</small>
            </span>
            <span>
              {data.awaiting}
              <small>Awaiting evidence</small>
            </span>
          </div>
          <p>
            Requests are saved and text is prepared. Evidence assessment is not connected in this
            sprint.
          </p>
          <button onClick={() => navigate("submissions")}>
            Review submissions <ArrowUpRight size={16} />
          </button>
        </section>
        <section className="admin-panel">
          <h2>Start with what matters</h2>
          {[
            ["People & access", "Review accounts and manage active status.", "users"],
            ["News desk", "Publish sourced updates for your readers.", "news"],
            ["Database & system", "Inspect connection health and migrations.", "system"],
          ].map(([title, description, target]) => (
            <button className="admin-quick" key={title} onClick={() => navigate(target as Tab)}>
              <span>
                <strong>{title}</strong>
                <small>{description}</small>
              </span>
              <ArrowUpRight size={18} />
            </button>
          ))}
        </section>
      </div>
    </>
  );
}
