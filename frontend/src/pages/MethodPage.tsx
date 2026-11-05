import { ArrowUpRight, ShieldCheck } from "lucide-react";

import { stages } from "../data/sampleInvestigation";

import { Note } from "../components/UI";
export function MethodPage() {
  return (
    <div className="page-container">
      <div className="method-hero">
        <span className="eyebrow">THE TRUTHLENS APPROACH</span>
        <h1>
          Show the reasoning.
          <br />
          <em>Keep the human judgment.</em>
        </h1>
        <p>
          Our intended workflow follows each claim from the original article to its evidence and
          context. Every step should leave a trail you can inspect.
        </p>
      </div>
      <div className="method-steps">
        {stages.map((s, i) => (
          <div key={s}>
            <span>{String(i + 1).padStart(2, "0")}</span>
            <div>
              <h2>{s}</h2>
              <p>
                {
                  [
                    "Preserve the words, link and surrounding context.",
                    "Separate factual statements from opinions and predictions.",
                    "Search for supporting and contradicting information independently.",
                    "Check origin, relevance and independent corroboration.",
                    "Make dates, disagreements and missing information visible.",
                    "Explain the finding, confidence and limits for every claim.",
                  ][i]
                }
              </p>
            </div>
            <ArrowUpRight size={18} />
          </div>
        ))}
      </div>
      <div className="principles-grid">
        {[
          [
            "No evidence ≠ false",
            "An incomplete search and an evidence-based contradiction are different things.",
          ],
          [
            "History is a signal",
            "Previous findings must be reassessed; current claim-specific evidence comes first.",
          ],
          [
            "Uncertainty stays visible",
            "Mixed or insufficient evidence should remain visible in the report.",
          ],
        ].map(([t, p]) => (
          <section className="panel" key={t}>
            <ShieldCheck size={23} />
            <h3>{t}</h3>
            <p>{p}</p>
          </section>
        ))}
      </div>
      <Note>
        The AI pipeline is planned. This release implements registration and demonstrates the wider
        product experience.
      </Note>
    </div>
  );
}
