import { ArrowRight } from "lucide-react";
import { go } from "../lib/navigation";

import { Note, SectionTitle } from "../components/UI";
export function HelpPage() {
  return (
    <div className="page-container">
      <SectionTitle
        tag="GETTING STARTED"
        title="A clearer way to use TruthLens"
        description="Save submissions, revisit your reports and understand the evidence."
      />
      <div className="help-grid">
        <section className="panel help-card">
          <h2>Keep your reports together.</h2>
          <p>
            Create an account and sign in. Text you submit while signed in is saved privately to
            your verification history.
          </p>
          <button className="button primary" onClick={() => go("library")}>
            My verification history <ArrowRight size={16} />
          </button>
        </section>
        <section className="panel help-card">
          <h2>Find what you need.</h2>
          <p>
            Search by title, choose a processing status, or filter by submission date. Open any
            available report to see its submission and findings.
          </p>
          <button className="button quiet" onClick={() => go("verify")}>
            Submit an article <ArrowRight size={16} />
          </button>
        </section>
        <section className="panel help-card">
          <h2>Understand a report.</h2>
          <p>
            Each report keeps the original submission, findings, evidence and context together. An
            awaiting-verification status means that no fact-check has been performed yet.
          </p>
          <button className="text-button" onClick={() => go("method")}>
            Our methodology <ArrowRight size={16} />
          </button>
        </section>
        <section className="panel help-card">
          <h2>Keep your account private.</h2>
          <p>
            Sign out after using a shared device. Your history is restricted to your account, and
            signing out ends access from the current session.
          </p>
        </section>
      </div>
      <Note>
        Article links can be imported. AI evidence verification is not connected yet. The fictional
        case study demonstrates how an evidence-based report can look.
      </Note>
    </div>
  );
}
