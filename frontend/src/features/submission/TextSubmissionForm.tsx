import { useEffect, useState } from "react";
import { ArrowRight, ArrowUpRight, CheckCircle2, FileText, Link, LockKeyhole } from "lucide-react";
import { api } from "../../api/client";
import { useAuth } from "../auth/AuthContext";
import {
  submitVerification,
  submitArticleUrl,
  type SubmissionAccepted,
} from "../../api/submissionApi";
import { openStoredReport, statusLabels } from "../history/historyApi";
import { sampleText } from "../../data/sampleInvestigation";
import { type VerificationReport } from "../../types/report";
import { go } from "../../lib/navigation";
import { validateArticleText, validateArticleUrl } from "./validation";
export function TextSubmissionForm() {
  const { user, refresh, loading: authLoading } = useAuth();
  const [tab, setTab] = useState<"text" | "url">("text"),
    [text, setText] = useState(""),
    [url, setUrl] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const [limit, setLimit] = useState<number | null>(null),
    [settingsError, setSettingsError] = useState(""),
    [settingsRetry, setSettingsRetry] = useState(0);
  const [accepted, setAccepted] = useState<SubmissionAccepted | null>(null),
    [report, setReport] = useState<VerificationReport | null>(null),
    [pollError, setPollError] = useState(""),
    [pollRetry, setPollRetry] = useState(0);
  useEffect(() => {
    let live = true;
    setSettingsError("");
    api<{ maxCodePoints: number }>("/api/config/submission")
      .then((config) => {
        if (live) setLimit(config.maxCodePoints);
      })
      .catch((e) => {
        if (live) setSettingsError(e.message);
      });
    return () => {
      live = false;
    };
  }, [settingsRetry]);
  useEffect(() => {
    if (!accepted) return;
    let live = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    setPollError("");
    const poll = async () => {
      try {
        const current = (await api("/api/verifications/" + accepted.id)) as VerificationReport;
        if (!live) return;
        setReport(current);
        if (current.status === "PROCESSING") timer = setTimeout(poll, 1000);
      } catch (e) {
        if (live) setPollError((e as Error).message);
      }
    };
    void poll();
    return () => {
      live = false;
      if (timer) clearTimeout(timer);
    };
  }, [accepted?.id, pollRetry]);
  const count = Array.from(text).length;
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy || limit === null) return;
    setError("");
    const validation = tab === "url" ? validateArticleUrl(url) : validateArticleText(text, limit);
    if (validation) {
      setError(validation);
      document.getElementById(tab === "url" ? "article-url" : "article-text")?.focus();
      return;
    }
    setBusy(true);
    try {
      const request =
        tab === "url"
          ? await submitArticleUrl(url, !!user)
          : await submitVerification(text, !!user);
      setAccepted(request);
      setReport(null);
    } catch (e) {
      setError((e as Error).message);
      if ((e as { status?: number }).status === 401) await refresh();
    } finally {
      setBusy(false);
    }
  }
  if (accepted)
    return (
      <section
        className="card submission submission-confirmation"
        aria-labelledby="submission-confirmed-title"
      >
        <CheckCircle2 size={34} />
        <span className="eyebrow">SUBMISSION RECEIVED</span>
        <h2 id="submission-confirmed-title">Processing started</h2>
        <p role="status">{accepted.message}</p>
        {accepted.originalUrl && (
          <p className="submission-source">
            Source:{" "}
            <a href={accepted.originalUrl} target="_blank" rel="noreferrer">
              {accepted.originalUrl}
            </a>
          </p>
        )}
        {accepted.extractionNote && (
          <p className="notice submission-import-note">{accepted.extractionNote}</p>
        )}
        <div className="submission-request-id">
          <span>Request ID</span>
          <code>{accepted.id}</code>
        </div>
        <div className="submission-real-status" aria-live="polite">
          <strong>{statusLabels[report?.status || accepted.status] || "Processing"}</strong>
          <p>
            {report?.stage === "TEXT_PREPARED"
              ? "Your text is prepared. Evidence verification has not been performed yet."
              : "Your text is being prepared for verification."}
          </p>
          {typeof report?.textMetrics?.characters === "number" && (
            <small>
              {report.textMetrics.characters.toLocaleString()} characters ·{" "}
              {report.textMetrics.words.toLocaleString()} words
            </small>
          )}
        </div>
        {pollError && (
          <p className="error" role="alert">
            {pollError}{" "}
            <button className="text-button" onClick={() => setPollRetry((n) => n + 1)}>
              Check again
            </button>
          </p>
        )}
        <div className="submission-confirm-actions">
          <button className="primary" onClick={() => openStoredReport(accepted.id)}>
            Open {report?.reportAvailable ? "report" : "request"} <ArrowRight size={17} />
          </button>
          <button
            className="text-button"
            onClick={() => {
              setAccepted(null);
              setReport(null);
              setText("");
              setUrl("");
              setError("");
            }}
          >
            Submit another article
          </button>
        </div>
        <p className="submission-access-note">
          {user
            ? "This request is saved in your personal verification history."
            : "This request is private to your browser session. Sign in before future submissions to keep them in personal history."}
        </p>
      </section>
    );
  return (
    <section className="card submission">
      <div className="card-heading">
        <div>
          <h2>New verification</h2>
          <p>Paste article text or import a public article link.</p>
        </div>
        <span className="step-tag">01 / SUBMIT</span>
      </div>
      <div className="input-tabs" role="tablist" aria-label="Input type">
        {(["text", "url"] as const).map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={tab === t}
            className={tab === t ? "selected" : ""}
            disabled={busy}
            onClick={() => {
              setTab(t);
              setError("");
            }}
          >
            {t === "text" ? <FileText size={16} /> : <Link size={16} />}Article{" "}
            {t === "text" ? "text" : "URL"}
          </button>
        ))}
      </div>
      <form onSubmit={submit} noValidate aria-busy={busy}>
        {tab === "text" ? (
          <>
            <div className="field-heading">
              <label htmlFor="article-text">
                Article text <span>*</span>
              </label>
              <button
                type="button"
                className="text-button"
                disabled={busy}
                onClick={() => {
                  setText(sampleText);
                  setError("");
                }}
              >
                Try an example <ArrowUpRight size={14} />
              </button>
            </div>
            <textarea
              id="article-text"
              value={text}
              disabled={busy}
              onChange={(e) => {
                setText(e.target.value);
                setError("");
              }}
              aria-invalid={!!error || (limit !== null && count > limit)}
              aria-describedby="text-help input-limit input-error"
              placeholder="Paste your news article or copied text here…"
            />
            <div className="field-footer">
              <span id="text-help">Unicode characters, including spaces.</span>
              <span id="input-limit" className={limit !== null && count > limit ? "error" : ""}>
                {count.toLocaleString()} / {limit === null ? "…" : limit.toLocaleString()}
              </span>
            </div>
          </>
        ) : (
          <div className="url-box">
            <label htmlFor="article-url">Source article URL</label>
            <input
              id="article-url"
              type="url"
              value={url}
              disabled={busy}
              autoComplete="url"
              aria-invalid={!!error}
              aria-describedby="url-help input-error"
              onChange={(e) => {
                setUrl(e.target.value);
                setError("");
              }}
              placeholder="https://example.com/article"
            />
            <small id="url-help">
              HTTP or HTTPS · maximum 2,048 characters. Public articles only; pages requiring
              sign-in may need pasted text.
            </small>
          </div>
        )}
        {settingsError && (
          <p role="alert" className="error">
            Unable to load the input limit.{" "}
            <button
              type="button"
              className="text-button"
              onClick={() => setSettingsRetry((n) => n + 1)}
            >
              Retry
            </button>
          </p>
        )}
        {error && (
          <p id="input-error" role="alert" className="error inline-error">
            {error}
          </p>
        )}
        {error && tab === "url" && (
          <button
            type="button"
            className="text-button"
            onClick={() => {
              setTab("text");
              setError("");
              requestAnimationFrame(() => document.getElementById("article-text")?.focus());
            }}
          >
            Paste article text instead <ArrowRight size={15} />
          </button>
        )}
        <div className="form-bottom">
          <div>
            <LockKeyhole size={15} />
            <span>
              {user ? "Saved privately to your history." : "Private to this browser session."}
            </span>
          </div>
          <button
            type="submit"
            className="primary"
            disabled={busy || limit === null || authLoading}
          >
            {busy
              ? tab === "url"
                ? "Fetching article…"
                : "Submitting…"
              : limit === null
                ? "Loading settings…"
                : "Start verification"}
            <ArrowRight size={17} />
          </button>
        </div>
      </form>
      {!user && (
        <p className="submission-signin-note">
          <button className="text-button" onClick={() => go("login")}>
            Sign in
          </button>{" "}
          before submitting to keep a personal history.
        </p>
      )}
    </section>
  );
}
