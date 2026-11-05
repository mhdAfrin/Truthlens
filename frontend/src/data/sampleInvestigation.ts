import type { VerificationReport } from "../types/report";
export const sampleText =
  "Riverton will offer free bus travel on weekends from 1 October. The scheme covers every bus route in the region. It will run for six months.";
export const sampleReport: VerificationReport = {
  id: "TL-SAMPLE-01",
  title: "Free weekend buses: what does the announcement actually say?",
  content: sampleText,
  date: "2026-10-01",
  sample: true,
  claims: [
    {
      id: 1,
      text: "Weekend bus travel will be free from 1 October.",
      finding: "Supported",
      explanation:
        "In this fictional example, the transport authority’s announcement agrees with the date and weekend fare described in the claim.",
      support: ["The sample announcement describes a weekend fare of zero from 1 October."],
      against: [],
      context: "The example concerns weekends only. It makes no claim about weekday fares.",
      confidence: "Illustrative assessment only; no confidence method has been applied.",
    },
    {
      id: 2,
      text: "The scheme covers every bus route in the region.",
      finding: "Mixed evidence",
      explanation:
        "The fictional headline says “every route”, but the accompanying sample terms exclude privately operated services. This change in scope matters.",
      support: ["A sample news headline describes the scheme as region-wide."],
      against: ["The fictional eligibility terms exclude privately operated routes."],
      context:
        "A broad headline can hide an important exception. Compare the exact scope of each source.",
      confidence: "Unresolved scope disagreement in the fictional evidence.",
    },
    {
      id: 3,
      text: "The scheme will run for six months.",
      finding: "Insufficient evidence",
      explanation:
        "The sample source gives a start date but no end date. Missing information is not proof that the claim is false.",
      support: [],
      against: [],
      context: "End date unavailable in this fictional example.",
      confidence: "An end-date assessment is unavailable.",
    },
  ],
};
export function createPreview(content: string): VerificationReport {
  return {
    id: "TL-PREVIEW-" + Date.now().toString(36).toUpperCase(),
    title: content.trim().slice(0, 90),
    content,
    date: new Date().toISOString(),
    sample: false,
    claims: content
      .trim()
      .split(/(?<=[.!?])\s+/)
      .filter(Boolean)

      .map((text, i) => ({
        id: i + 1,
        text,
        finding: "Not assessed",
        explanation: "Your text has not been verified. No evidence has been retrieved or assessed.",
        support: [],
        against: [],
        context:
          "Dates, source reliability, conflicts and confidence are unavailable until the verification services are connected.",
        confidence: "Not assessed",
      })),
  };
}
export const stages = [
  "Read the article",
  "Identify the claims",
  "Look for evidence",
  "Compare the sources",
  "Review the context",
  "Build a clear report",
];
