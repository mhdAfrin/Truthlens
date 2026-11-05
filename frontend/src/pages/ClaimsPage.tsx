import { useState } from "react";
import { ArrowUpRight, Search } from "lucide-react";

import { type VerificationReport } from "../types/report";
import { Badge, Empty, Note, SectionTitle } from "../components/UI";
export function ClaimsPage({
  reports,
  openReport,
}: {
  reports: VerificationReport[];
  openReport: (r: VerificationReport) => void;
}) {
  const [q, setQ] = useState(""),
    [filter, setFilter] = useState("All findings");
  const claims = reports
    .flatMap((r) => r.claims.map((c) => ({ r, c })))
    .filter(
      ({ c }) =>
        c.text.toLowerCase().includes(q.toLowerCase()) &&
        (filter === "All findings" || filter === c.finding),
    );
  return (
    <div className="page-container">
      <SectionTitle
        tag="ONE STATEMENT AT A TIME"
        title="The claim explorer."
        description="Big stories contain smaller statements. Explore them individually."
      />
      <div className="library-toolbar">
        <div className="search-field">
          <Search size={17} />
          <input
            aria-label="Search claims"
            placeholder="Find a statement"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <select
          aria-label="Filter finding"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          {[
            "All findings",
            "Supported",
            "Mixed evidence",
            "Insufficient evidence",
            "Not assessed",
          ].map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
      </div>
      <div className="claims-grid">
        {claims.map(({ r, c }) => (
          <button className="claim-card panel" key={r.id + c.id} onClick={() => openReport(r)}>
            <div className="section-heading">
              <span className="eyebrow">{r.sample ? "FICTIONAL EXAMPLE" : "UNASSESSED TEXT"}</span>
              <ArrowUpRight size={16} />
            </div>
            <h3>{c.text}</h3>
            <Badge finding={c.finding} />
            <p>
              {r.id} · Claim {c.id}
            </p>
          </button>
        ))}
      </div>
      {!claims.length && (
        <Empty title="No matching statements" description="Try a different word or finding." />
      )}
      <Note>
        Sample findings are fictional. Your submitted text is labelled Not assessed until the engine
        is connected.
      </Note>
    </div>
  );
}
