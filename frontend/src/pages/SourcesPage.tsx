import { useState } from "react";
import type { VerificationReport } from "../types/report";
import { ArrowRight, Clock, Globe, Search } from "lucide-react";

import { Empty, Note, SectionTitle } from "../components/UI";
export function SourcesPage({ reports = [] }: { reports?: VerificationReport[] }) {
  const [q, setQ] = useState(""),
    [expanded, setExpanded] = useState(false);
  const visible = "fictional riverton transport authority".includes(q.toLowerCase());
  return (
    <div className="page-container">
      <SectionTitle
        tag="FOLLOW THE INFORMATION"
        title="Good evidence has a beginning."
        description="See a source’s origin, its assessment method, and what we still don’t know."
      />
      {reports.flatMap((r) => r.claims.flatMap((c) => c.evidence || [])).filter((e, i, all) => all.findIndex((x) => x.url === e.url) === i).filter((e) => (e.title + e.publisher).toLowerCase().includes(q.toLowerCase())).map((source) => (
        <div className="source-record panel" key={source.url}>
          <span className="eyebrow">DISCOVERED SOURCE · {source.sourceType}</span>
          <h2><a href={source.url} target="_blank" rel="noreferrer">{source.title} ↗</a></h2>
          <p>{source.snippet}</p>
          <p>{source.publisher} · {source.relationship} · Date: {source.publicationDate}</p>
        </div>
      ))}
      <div className="source-library-banner">
        <span>
          <Globe size={35} />
        </span>
        <div>
          <h2>Credibility deserves an explanation.</h2>
          <p>TruthLens will show why a source was assessed—not just give it a score.</p>
        </div>
        <span className="outline-label">METHOD PENDING</span>
      </div>
      <div className="library-toolbar">
        <div className="search-field">
          <Search size={17} />
          <input
            aria-label="Search source library"
            placeholder="Search a source or publisher"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <span className="fine-print">1 fictional record · No live assessments</span>
      </div>
      {visible ? (
        <div className="source-record panel">
          <div>
            <span className="source-monogram">RT</span>
            <div>
              <span className="eyebrow">FICTIONAL SOURCE</span>
              <h2>Riverton Transport Authority</h2>
              <p>A placeholder for the case study. No real publisher is being rated.</p>
            </div>
            <span className="badge not-assessed">Not assessed</span>
          </div>
          <div className="metadata-grid">
            {[
              ["Source type", "Primary in the fictional case"],
              ["Assessment method", "Not applied"],
              ["Assessment date", "Unavailable"],
              ["Independent corroboration", "Not assessed"],
            ].map(([t, v]) => (
              <div key={t}>
                <span>{t}</span>
                <strong>{v}</strong>
              </div>
            ))}
          </div>
          <button
            className="text-button"
            aria-expanded={expanded}
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? "Hide assessment history" : "View assessment history"}{" "}
            <ArrowRight size={15} />
          </button>
          {expanded && (
            <div className="assessment-history">
              <Clock size={20} />
              <p>
                No assessment history exists yet. Future assessments will preserve dates, methods,
                reasons, references and earlier versions.
              </p>
            </div>
          )}
        </div>
      ) : (
        <Empty
          title="No matching source"
          description="Try Riverton to inspect the fictional source record."
        />
      )}
      <Note>
        Source origin, independence, relevance, and dates matter alongside reputation. Missing
        metadata is always marked unavailable.
      </Note>
    </div>
  );
}
