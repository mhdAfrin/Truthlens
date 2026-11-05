import type { NewsData } from "./adminTypes";
import { type AdminWorkspaceState } from "./useAdminWorkspace";
import { emptyNews, date } from "./adminHelpers";

export function AdminNewsDesk({ state }: { state: AdminWorkspaceState }) {
  const data = state.data as NewsData;
  const { busy, news, edit, setNews, setEdit, saveNews } = state;
  return (
    <div className="admin-two">
      <section className="admin-panel">
        <h2>{edit ? "Edit story" : "Add a story"}</h2>
        <form className="admin-news-form" onSubmit={saveNews}>
          <label>
            Headline{" "}
            <input
              required
              maxLength={180}
              value={news.title}
              onChange={(e) => setNews({ ...news, title: e.target.value })}
            />
          </label>
          <label>
            Summary{" "}
            <textarea
              maxLength={600}
              value={news.summary}
              onChange={(e) => setNews({ ...news, summary: e.target.value })}
            />
          </label>
          <label>
            Publisher{" "}
            <input
              required
              maxLength={100}
              value={news.publisher}
              onChange={(e) => setNews({ ...news, publisher: e.target.value })}
            />
          </label>
          <label>
            Original source URL{" "}
            <input
              required
              type="url"
              maxLength={2048}
              value={news.sourceUrl}
              onChange={(e) => setNews({ ...news, sourceUrl: e.target.value })}
            />
          </label>
          <label>
            Publication time (UTC)
            <input
              required
              type="datetime-local"
              value={news.publishedAt}
              onChange={(e) => setNews({ ...news, publishedAt: e.target.value })}
            />
          </label>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={news.visible}
              onChange={(e) => setNews({ ...news, visible: e.target.checked })}
            />
            Publish on home page
          </label>
          <label className="admin-check">
            <input
              type="checkbox"
              checked={news.breaking}
              onChange={(e) => setNews({ ...news, breaking: e.target.checked })}
            />
            Developing story
          </label>
          <button className="primary" disabled={busy}>
            {busy ? "Saving…" : "Save story"}
          </button>
          {edit && (
            <button
              type="button"
              onClick={() => {
                setEdit(null);
                setNews(emptyNews());
              }}
            >
              Cancel editing
            </button>
          )}
        </form>
      </section>
      <section className="admin-panel">
        <h2>Editorial queue</h2>
        <p>Publisher feed stories are separate from your curated selections.</p>
        {!data.items.length && <p className="admin-empty">Your first story starts here.</p>}
        {data.items.map((n) => (
          <article className="admin-news-item" key={n.id}>
            <span className={`admin-pill ${n.visible ? "good" : ""}`}>
              {n.visible ? "Published" : "Draft"}
            </span>
            <h3>{n.title}</h3>
            <p>
              {n.publisher} · {date(n.publishedAt)}
            </p>
            <button
              onClick={() => {
                setEdit(n.id);
                setNews({
                  title: n.title,
                  summary: n.summary,
                  publisher: n.publisher,
                  sourceUrl: n.sourceUrl,
                  visible: n.visible,
                  breaking: n.breaking,
                  publishedAt: new Date(n.publishedAt).toISOString().slice(0, 16),
                });
              }}
            >
              Edit story
            </button>
          </article>
        ))}
      </section>
    </div>
  );
}
