import { statusLabels } from "../features/history/historyApi";
import { useEffect, useState } from "react";
import {
  ArrowLeft,
  ArrowUpRight,
  Bookmark,
  ChevronDown,
  Download,
  Globe,
  Info,
  Printer,
  Scale,
  ShieldCheck,
} from "lucide-react";
import { go } from "../lib/navigation";
import type { VerificationReport } from "../types/report";
import { Badge, Note } from "../components/UI";
export function ReportPage({
  report,
  saved,
  toggleSaved,
}: {
  report: VerificationReport;
  saved: boolean;
  toggleSaved: () => void;
}) {
  const [index, setIndex] = useState(0),
    [tab, setTab] = useState("Overview");
  useEffect(() => {
    setIndex(0);
    setTab("Overview");
  }, [report.id]);
  const claim = report.claims[index];
  const sample = report.sample;
  function download() {
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(report, null, 2)], { type: "application/json" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = report.id + ".json";
    a.click();
    URL.revokeObjectURL(url);
  }
  return (
    <div className="page-container report-page">
      <div className="report-toolbar">
        <button
          className="text-button"
          onClick={() => go(!report.stored || report.accountOwned === false ? "verify" : "library")}
        >
          <ArrowLeft size={16} />
          {!report.stored || report.accountOwned === false ? "New verification" : "Back to history"}
        </button>
        <div>
          {!report.stored && (
            <button className="button quiet" onClick={toggleSaved} aria-pressed={saved}>
              <Bookmark size={15} fill={saved ? "currentColor" : "none"} />
              {saved ? "Bookmarked this session" : "Bookmark this session"}
            </button>
          )}
          <button className="button quiet" onClick={() => window.print()}>
            <Printer size={15} />
            Print / PDF
          </button>
          <button className="button quiet" onClick={download}>
            <Download size={15} />
            Export JSON
          </button>
        </div>
      </div>
      <div className="report-heading">
        <span className="eyebrow">
          {sample
            ? "FICTIONAL CASE STUDY"
            : report.stored
              ? "YOUR VERIFICATION REPORT"
              : "YOUR TEXT"}
        </span>
        <h1>{report.title}</h1>
        {report.extractionNote && (
          <p className="notice submission-import-note">{report.extractionNote}</p>
        )}
        {report.originalUrl && /^https?:\/\//i.test(report.originalUrl) && (
          <p className="submission-source">
            Original article:{" "}
            <a href={report.originalUrl} target="_blank" rel="noreferrer">
              {report.originalUrl}
            </a>
          </p>
        )}
        <p>
          {report.id} <span>·</span>{" "}
          {new Date(report.date).toLocaleDateString("en", {
            day: "numeric",
            month: "long",
            year: "numeric",
          })}{" "}
          <span>·</span> {report.claims.length} passages
        </p>
      </div>
      {report.status && (
          <div>
            <p>
      <span className={"processing-status " + report.status.toLowerCase()}>
        {statusLabels[report.status] || report.status}
      </span>
            </p>

            {report.status === "PROCESSING" && report.stage && (
                <p role="status">
                  Current analysis stage: {report.stage.replace(/_/g, " ")}
                </p>
            )}
          </div>
      )}
      <div className="report-disclaimer">
        <Info size={17} />
        {sample ? "This is a fictional example, not a live verification." : report.status === "COMPLETED" ? "Claim extraction and public-source discovery finished. Search results are not proof; any unresolved claims remain Insufficient evidence or Not assessed." : report.status === "FAILED" ? "Verification failed. Available partial claims are shown below." : "Verification is still in progress. No final verdict is available."}
      </div>
      {report.error && <p className="error" role="alert">{report.error}</p>}
      <div className="report-layout">
        <aside className="claim-navigation">
          <div className="section-heading">
            <h2>{sample ? "The claims" : "Extracted claims"}</h2>
            <span>{report.claims.length}</span>
          </div>
          {report.claims.map((c, i) => (
            <button
              key={c.id}
              className={index === i ? "selected" : ""}
              onClick={() => {
                setIndex(i);
                setTab("Overview");
              }}
              aria-current={index === i ? "true" : undefined}
            >
              <span className="claim-number">{String(i + 1).padStart(2, "0")}</span>
              <p>{c.text}</p>
              <Badge finding={c.finding} />
            </button>
          ))}
          <div className="claim-nav-note">
            <ShieldCheck size={17} />
            <p>
              {sample
                ? "Each statement has its own finding. A single label can hide important differences."
                : "Your original submission is preserved. Findings and evidence will appear when verification is performed."}
            </p>
          </div>
        </aside>
        <div className="claim-workspace">
          <div className="mobile-claim-select">
            <label htmlFor="claim-select">{sample ? "Choose a claim" : "Choose a passage"}</label>
            <select
              id="claim-select"
              value={index}
              onChange={(e) => {
                setIndex(Number(e.target.value));
                setTab("Overview");
              }}
            >
              {report.claims.map((c, i) => (
                <option key={c.id} value={i}>
                  {i + 1}. {c.text}
                </option>
              ))}
            </select>
          </div>
          {claim && (
            <article className="claim-panel panel">
              <div className="claim-panel-head">
                <span className="eyebrow">
                  {sample ? "CLAIM" : "EXTRACTED CLAIM"} {String(index + 1).padStart(2, "0")} /{" "}
                  {String(report.claims.length).padStart(2, "0")}
                </span>
                <Badge finding={claim.finding} />
              </div>
              <h2>{claim.text}</h2>
              {!sample && <p className="fine-print">Type: {claim.claimType || "Unclassified"} · Original passage: {claim.originalPassage || claim.text}</p>}
              <div className="report-tabs" role="tablist" aria-label="Claim details">
                {["Overview", "Evidence", "Context & dates", "Sources"].map((t) => (
                  <button
                    key={t}
                    role="tab"
                    aria-selected={tab === t}
                    onClick={() => setTab(t)}
                    className={tab === t ? "selected" : ""}
                  >
                    {t}
                    {t === "Evidence" && <span>{claim.support.length + claim.against.length}</span>}
                  </button>
                ))}
              </div>
              <div className="report-tab-panel" role="tabpanel">
                {tab === "Overview" && (
                  <>
                    <span className="eyebrow">
                      {sample ? "EXAMPLE EXPLANATION" : "VERIFICATION STATUS"}
                    </span>
                    <p className="explanation-text">{claim.explanation}</p>
                    <div className="insight-box">
                      <span className="proof-icon amber">
                        <Scale size={19} />
                      </span>
                      <div>
                        <h3>What to keep in mind</h3>
                        <p>{claim.context}</p>
                      </div>
                    </div>
                    <div className="confidence-row">
                      <span>
                        <ShieldCheck size={17} />
                        Assessment confidence
                      </span>
                      <strong>{sample ? "Illustrative only" : claim.confidence}</strong>
                    </div>
                    <p className="fine-print">
                      {claim.confidence} Confidence is not a guarantee of correctness.
                    </p>
                    <details className="original-passage">
                      <summary>
                        Read the original submission <ChevronDown size={15} />
                      </summary>
                      <blockquote>{report.content}</blockquote>
                    </details>
                  </>
                )}
                {tab === "Evidence" && (
                  <>
                    <div className="evidence-intro">
                      <Globe size={18} />
                      <p>
                        {sample
                          ? "Fictional passages show how both sides of a claim will be presented."
                          : `Evidence search: ${claim.searchStatus || "Not started"}. ${claim.searchError || "Search hits are contextual leads, not confirmed support."}`}
                      </p>
                    </div>
                    {!sample && (claim.evidence || []).map((source) => (
                      <div className="evidence-card" key={source.url}>
                        <span className="eyebrow">{source.sourceType} · {source.relationship}</span>
                        <h3><a href={source.url} target="_blank" rel="noreferrer">{source.title} ↗</a></h3>
                        <p>{source.snippet}</p>
                        <small>{source.publisher} · Publication date: {source.publicationDate}</small>
                      </div>
                    ))}
                    {[
                      ["Supporting evidence", claim.support, "support"],
                      ["Contradicting evidence", claim.against, "against"],
                    ].map(([title, items, type]) => (
                      <section className={"evidence-section " + type} key={title as string}>
                        <h3>
                          {title as string}
                          <span>{(items as string[]).length}</span>
                        </h3>
                        {(items as string[]).length ? (
                          (items as string[]).map((text, i) => (
                            <div className="evidence-card" key={text}>
                              <span className="eyebrow">
                                FICTIONAL EXCERPT ·{" "}
                                {type === "support"
                                  ? "SUPPORTING PASSAGE"
                                  : "SAMPLE ELIGIBILITY TERMS"}
                              </span>
                              <blockquote>“{text}”</blockquote>
                              <div>
                                <span>
                                  Source type:{" "}
                                  {type === "support" ? "Unknown (illustrative)" : "Unknown"}
                                </span>
                                <span>Publication date: unavailable</span>
                              </div>
                            </div>
                          ))
                        ) : (
                          <p className="empty-evidence">
                            {sample
                              ? "No passage is included for this side of the fictional example."
                              : "No evidence has been retrieved."}
                          </p>
                        )}
                      </section>
                    ))}
                    <Note>
                      A missing contradicting passage does not prove a claim is supported. Evidence
                      must be relevant, current, and independent.
                    </Note>
                  </>
                )}
                {tab === "Context & dates" && (
                  <>
                    <span className="eyebrow">CONTEXT MATTERS</span>
                    <h3>Look beyond the sentence.</h3>
                    <p>{claim.context}</p>
                    <div className="metadata-grid">
                      {[
                        ["Publication date", "Unavailable"],
                        ["Last updated", "Unavailable"],
                        ["Time period", sample ? "Fictional October scheme" : "Not assessed"],
                        [
                          "Conflict status",
                          sample && index === 1 ? "Scope disagreement (sample)" : "Not assessed",
                        ],
                      ].map(([t, v]) => (
                        <div key={t}>
                          <span>{t}</span>
                          <strong>{v}</strong>
                        </div>
                      ))}
                    </div>
                    <Note>
                      Old evidence is not automatically wrong. Its relevance depends on the time and
                      context of the claim.
                    </Note>
                  </>
                )}
                {tab === "Sources" && (
                  <>
                    <span className="eyebrow">SOURCE TRANSPARENCY</span>
                    <h3>Know where the information begins.</h3>
                    {sample ? (
                      <div className="source-preview">
                        <span>
                          <Globe size={24} />
                        </span>
                        <div>
                          <h3>Fictional Riverton Transport Authority</h3>
                          <p>Example only · No real source or quotation is represented.</p>
                          <small>Credibility: not assessed · Method: not applied</small>
                        </div>
                      </div>
                    ) : (
                        <div>
                          {(claim.evidence || []).length > 0 ? (
                              <>
                                <p>
                                  Retrieved sources for this claim. Search results are not
                                  automatically verified evidence.
                                </p>

                                {Array.from(
                                    new Map(
                                        (claim.evidence || []).map((source) => [
                                          source.url.trim().replace(/\/$/, "").toLowerCase(),
                                          source,
                                        ])
                                    ).values()
                                ).map((source) => (
                                    <div className="evidence-card" key={source.url}>
          <span className="eyebrow">
            {source.sourceType || "Unclassified"} ·{" "}
            {source.relationship || "Contextual source"}
          </span>

                                      <h3>
                                        <a
                                            href={source.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                          {source.title || source.url} ↗
                                        </a>
                                      </h3>

                                      <p>{source.snippet || "No description available."}</p>

                                      <small>
                                        Publisher: {source.publisher || "Unknown"}
                                        {" · "}
                                        Publication date: {source.publicationDate || "Unavailable"}
                                      </small>

                                      <p className="fine-print">
                                        Source URL:{" "}
                                        <a
                                            href={source.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            style={{ overflowWrap: "anywhere" }}
                                        >
                                          {source.url}
                                        </a>
                                      </p>
                                    </div>
                                ))}
                              </>
                          ) : (
                              <p>
                                {claim.searchError
                                    ? `Source search failed: ${claim.searchError}`
                                    : "No sources were retrieved for this claim."}
                              </p>
                          )}
                        </div>
                    )}
                    <button className="text-button" onClick={() => go("sources")}>
                      Explore the source library <ArrowUpRight size={15} />
                    </button>
                    <Note>
                      A source’s reputation is context—not proof that a particular claim is correct.
                    </Note>
                  </>
                )}
              </div>
            </article>
          )}
        </div>
      </div>
      <div className="print-report">
        <h2>Complete report · {report.id}</h2>
        {report.claims.map((c) => (
          <article key={c.id}>
            <h3>{c.text}</h3>
            <p>{c.finding}</p>
            <p>{c.explanation}</p>
            <h4>Supporting evidence</h4>
            <p>{c.support.join(" ") || "None available."}</p>
            <h4>Contradicting evidence</h4>
            <p>{c.against.join(" ") || "None available."}</p>
            <p>{c.context}</p>
            <p>{c.confidence}</p>
          </article>
        ))}
      </div>
    </div>
  );
}
