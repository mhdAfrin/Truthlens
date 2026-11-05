import { useEffect, useState } from "react";
import { ArrowLeft, ArrowRight, FileCheck2, History, Plus, Search } from "lucide-react";
import { api } from "../api/client";
import { useAuth } from "../features/auth/AuthContext";
import { openStoredReport, statusLabels, type HistoryResult } from "../features/history/historyApi";
import { go } from "../lib/navigation";
const empty = { q: "", from: "", to: "", status: "" };
export function LibraryPage() {
  const { user, refresh } = useAuth();
  const [draft, setDraft] = useState(empty),
    [filters, setFilters] = useState(empty),
    [page, setPage] = useState(0),
    [result, setResult] = useState<HistoryResult | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [retry, setRetry] = useState(0);
  useEffect(() => {
    let live = true;
    setLoading(true);
    setError("");
    const params = new URLSearchParams({ page: String(page), size: "10" });
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    api<HistoryResult>("/api/verifications?" + params)
      .then((data) => {
        if (live) setResult(data);
      })
      .catch(async (e) => {
        if (!live) return;
        setError(e.message);
        setResult(null);
        if (e.status === 401) await refresh();
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [user?.id, filters, page, retry]);
  function apply(e: React.FormEvent) {
    e.preventDefault();
    if (draft.from && draft.to && draft.from > draft.to) {
      setError("The start date must be on or before the end date.");
      return;
    }
    setPage(0);
    setFilters({ ...draft });
  }
  const filtered = Object.values(filters).some(Boolean);
  return (
    <div className="page-container personal-history">
      <div className="history-heading">
        <div>
          <span className="eyebrow">YOUR WORKSPACE</span>
          <h1>Verification history</h1>
          <p>Your submissions, reports and their latest processing status.</p>
        </div>
        <button className="button primary" onClick={() => go("verify")}>
          <Plus size={17} />
          New verification
        </button>
      </div>
      <form className="history-filters panel" onSubmit={apply}>
        <div className="history-search">
          <label htmlFor="history-search">Search submissions</label>
          <div>
            <Search size={17} />
            <input
              id="history-search"
              type="search"
              maxLength={120}
              placeholder="Search by title…"
              value={draft.q}
              onChange={(e) => setDraft({ ...draft, q: e.target.value })}
            />
          </div>
        </div>
        <div>
          <label htmlFor="history-status">Processing status</label>
          <select
            id="history-status"
            value={draft.status}
            onChange={(e) => setDraft({ ...draft, status: e.target.value })}
          >
            <option value="">All statuses</option>
            {Object.entries(statusLabels).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="history-from">From date (UTC)</label>
          <input
            id="history-from"
            type="date"
            value={draft.from}
            max={draft.to || undefined}
            onChange={(e) => setDraft({ ...draft, from: e.target.value })}
          />
        </div>
        <div>
          <label htmlFor="history-to">To date (UTC)</label>
          <input
            id="history-to"
            type="date"
            value={draft.to}
            min={draft.from || undefined}
            onChange={(e) => setDraft({ ...draft, to: e.target.value })}
          />
        </div>
        <div className="history-filter-actions">
          <button type="submit" className="button primary">
            Apply filters
          </button>
          <button
            type="button"
            className="text-button"
            onClick={() => {
              setDraft(empty);
              setFilters({ ...empty });
              setPage(0);
              setError("");
            }}
          >
            Reset
          </button>
        </div>
      </form>
      {error && (
        <div className="registration-error" role="alert">
          {error}{" "}
          <button className="text-button" onClick={() => setRetry((n) => n + 1)}>
            Try again
          </button>
        </div>
      )}
      {loading ? (
        <div className="history-loading" role="status">
          Loading your verification history…
        </div>
      ) : (
        result && (
          <>
            <p className="history-count" role="status">
              {result.totalElements} {result.totalElements === 1 ? "submission" : "submissions"}
              {filtered
                ? result.totalElements === 1
                  ? " matches your filters"
                  : " match your filters"
                : ""}
            </p>
            {result.items.length ? (
              <div className="history-records panel">
                {result.items.map((item) => (
                  <article className="history-record" key={item.id}>
                    <span className="history-file">
                      <FileCheck2 size={22} />
                    </span>
                    <div className="history-record-copy">
                      <h2>
                        <button onClick={() => openStoredReport(item.id)}>{item.title}</button>
                      </h2>
                      <time dateTime={item.createdAt}>
                        {new Date(item.createdAt).toLocaleString("en-GB", {
                          dateStyle: "medium",
                          timeStyle: "short",
                          timeZone: "UTC",
                        })}{" "}
                        UTC
                      </time>
                    </div>
                    <span className={"processing-status " + item.status.toLowerCase()}>
                      {statusLabels[item.status] || item.status}
                    </span>
                    <button className="button quiet" onClick={() => openStoredReport(item.id)}>
                      {item.reportAvailable ? "Open report" : "View request"}{" "}
                      <ArrowRight size={15} />
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              <div className="history-empty panel">
                <History size={34} />
                <h2>
                  {filtered ? "No submissions match these filters" : "Your history starts here"}
                </h2>
                <p>
                  {filtered
                    ? "Try another date range, status or search term."
                    : "Submit article text while signed in and your report will be saved here."}
                </p>
                <button
                  className="button primary"
                  onClick={() =>
                    filtered
                      ? (setDraft(empty), setFilters({ ...empty }), setPage(0))
                      : go("verify")
                  }
                >
                  {filtered ? "Clear filters" : "Start a verification"}
                  <ArrowRight size={16} />
                </button>
              </div>
            )}
            {result.totalPages > 1 && (
              <nav className="history-pagination" aria-label="History pages">
                <button
                  className="button quiet"
                  disabled={page === 0}
                  onClick={() => setPage((n) => n - 1)}
                >
                  <ArrowLeft size={15} />
                  Previous
                </button>
                <span>
                  Page {page + 1} of {result.totalPages}
                </span>
                <button
                  className="button quiet"
                  disabled={page + 1 >= result.totalPages}
                  onClick={() => setPage((n) => n + 1)}
                >
                  Next
                  <ArrowRight size={15} />
                </button>
              </nav>
            )}
          </>
        )
      )}
    </div>
  );
}
