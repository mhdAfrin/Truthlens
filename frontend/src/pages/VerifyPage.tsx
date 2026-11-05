import { NewsWatch } from "../features/submission/NewsWatch";
import { TextSubmissionForm } from "../features/submission/TextSubmissionForm";
import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  ChevronRight,
  Globe,
  ScanLine,
  Search,
  ShieldCheck,
} from "lucide-react";
import { go } from "../lib/navigation";
import { type VerificationReport } from "../types/report";
import { sampleReport } from "../data/sampleInvestigation";
export function VerifyPage({ openReport }: { openReport: (r: VerificationReport) => void }) {
  return (
    <div className="verify-page">
      <div className="page-heading">
        <div>
          <div className="eyebrow">EVIDENCE, IN PERSPECTIVE</div>
          <h1>
            Start with a claim.
            <br className="mobile-break" /> Follow the evidence.
          </h1>
          <p>Investigate the facts behind the news, one claim at a time.</p>
        </div>
        <span className="heading-mark">
          <ScanLine size={39} />
        </span>
      </div>
      <div className="submit-layout">
        <TextSubmissionForm />
        <aside className="guide">
          <section className="card how">
            <div className="eyebrow">FROM ARTICLE TO INSIGHT</div>
            <h2>A clearer view of the facts.</h2>
            <p>Claims, evidence and context in one place.</p>
            <div className="how-steps">
              {[
                ["01", "Identify the claims", "Separate factual statements from the noise."],
                ["02", "Compare both sides", "See supporting and challenging evidence."],
                ["03", "Keep the context", "Inspect dates, sources and uncertainty."],
              ].map(([n, t, d]) => (
                <div className="how-step" key={n}>
                  <span>{n}</span>
                  <div>
                    <h3>{t}</h3>
                    <p>{d}</p>
                  </div>
                </div>
              ))}
            </div>
            <button className="text-button" onClick={() => go("method")}>
              Explore our methodology <ArrowUpRight size={15} />
            </button>
          </section>
          <div className="notice">
            <ShieldCheck size={21} />
            <div>
              <h3>
                An investigation aid.
                <br />
                Human judgment comes first.
              </h3>
              <p>
                Sample findings are fictional. Your own text remains unassessed until AI
                verification is connected.
              </p>
            </div>
          </div>
        </aside>
      </div>
      <NewsWatch />
      <section className="recent-section">
        <div className="section-heading">
          <div>
            <h2>Explore an investigation</h2>
            <p>See how context changes a headline.</p>
          </div>
          <button className="text-button" onClick={() => go("library")}>
            View history <ArrowRight size={15} />
          </button>
        </div>
        <div className="card">
          <button className="recent-row" onClick={() => openReport(sampleReport)}>
            <span className="file-icon">
              <BookOpen size={20} />
            </span>
            <div>
              <strong>Free weekend buses. But which routes qualify?</strong>
              <small>Fictional case study · 3 claims · Evidence, context and sources</small>
            </div>
            <span className="status">Sample report</span>
            <ChevronRight size={17} />
          </button>
        </div>
      </section>
      <div className="workspace-extras">
        <button onClick={() => go("claims")}>
          <Search size={20} />
          <div>
            <strong>Explore individual claims</strong>
            <small>Find the detail behind the conclusion.</small>
          </div>
          <ArrowUpRight size={16} />
        </button>
        <button onClick={() => go("sources")}>
          <Globe size={20} />
          <div>
            <strong>Look closer at the sources</strong>
            <small>Understand origin, dates and limitations.</small>
          </div>
          <ArrowUpRight size={16} />
        </button>
      </div>
    </div>
  );
}
