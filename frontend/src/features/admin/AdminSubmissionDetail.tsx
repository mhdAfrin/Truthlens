import { type AdminWorkspaceState } from "./useAdminWorkspace";

export function AdminSubmissionDetail({ state }: { state: AdminWorkspaceState }) {
  const { detail, setDetail } = state;
  if (!detail) return null;
  return (
    <section className="admin-panel">
      <button className="admin-close" onClick={() => setDetail(null)}>
        Close details
      </button>
      <p className="admin-eyebrow">PRIVATE REQUEST · ACCESS RECORDED IN AUDIT LOG</p>
      <h2>{detail.title}</h2>
      <p>{detail.id}</p>
      {detail.originalUrl && <p>{detail.originalUrl}</p>}
      <pre className="admin-content">{detail.content}</pre>
    </section>
  );
}
