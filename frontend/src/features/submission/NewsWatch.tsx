import { useEffect, useState } from "react";
import { ArrowUpRight, Newspaper } from "lucide-react";
import { api } from "../../api/client";
type Story = {
  id: string;
  title: string;
  summary: string;
  sourceUrl: string;
  publisher: string;
  publishedAt: string;
  breaking: boolean;
  origin: string;
};
export function NewsWatch() {
  const [items, setItems] = useState<Story[]>([]),
    [range, setRange] = useState("recent"),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [retry, setRetry] = useState(0),
    [feedStatus, setFeedStatus] = useState("");
  useEffect(() => {
    let live = true;
    setLoading(true);
    setError("");
    api<{ items: Story[]; feedStatus: string }>("/api/news")
      .then((d) => {
        if (live) {
          setItems(d.items);
          setFeedStatus(d.feedStatus);
        }
      })
      .catch((e) => {
        if (live) setError(e.message);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [retry]);
  const recent = items.filter(
    (n) =>
      Date.now() - Date.parse(n.publishedAt) <= 72 * 3600 * 1000 &&
      Date.parse(n.publishedAt) <= Date.now(),
  );
  const shown = (range === "recent" ? recent : items).slice(0, 6);
  return (
    <section className="news-watch" aria-labelledby="news-watch-title">
      <div className="news-watch-heading">
        <div>
          <span className="eyebrow">IN THE NEWS</span>
          <h2 id="news-watch-title">Stories worth a closer look.</h2>
          <p>Independent fact-checks and sourced editorial selections.</p>
        </div>
        <div className="news-filter" role="group" aria-label="News date filter">
          <button
            aria-pressed={range === "recent"}
            className={range === "recent" ? "selected" : ""}
            onClick={() => setRange("recent")}
          >
            Past 3 days
          </button>
          <button
            aria-pressed={range === "latest"}
            className={range === "latest" ? "selected" : ""}
            onClick={() => setRange("latest")}
          >
            Latest available
          </button>
        </div>
      </div>
      {loading ? (
        <p className="news-empty" role="status">
          Loading stories…
        </p>
      ) : error ? (
        <div className="news-empty" role="alert">
          {error}{" "}
          <button className="text-button" onClick={() => setRetry((n) => n + 1)}>
            Try again
          </button>
        </div>
      ) : shown.length ? (
        <div className="news-grid">
          {shown.map((n) => (
            <article className="news-card" key={n.id}>
              <div className="news-meta">
                <Newspaper size={14} />
                {n.publisher}
                {n.breaking && <span className="news-developing">Developing</span>}
              </div>
              <h3>{n.title}</h3>
              <p>{n.summary}</p>
              <a href={n.sourceUrl} target="_blank" rel="noreferrer">
                Read original source <ArrowUpRight size={14} />
              </a>
              <small>
                {new Date(n.publishedAt).toLocaleDateString(undefined, {
                  month: "short",
                  day: "numeric",
                  year: "numeric",
                })}{" "}
                · {n.origin}
              </small>
            </article>
          ))}
        </div>
      ) : (
        <div className="news-empty">
          {range === "recent"
            ? "No published stories from the past 3 days are available yet."
            : "No stories are available right now."}
          {range === "recent" && items.length > 0 && (
            <button className="text-button" onClick={() => setRange("latest")}>
              Browse latest available stories <ArrowUpRight size={14} />
            </button>
          )}
        </div>
      )}
      <p className="news-note">
        Source dates are shown as published. External stories are not TruthLens verification
        results.{" "}
        {feedStatus.includes("unavailable") ? "The publisher feed is temporarily unavailable." : ""}
      </p>
    </section>
  );
}
