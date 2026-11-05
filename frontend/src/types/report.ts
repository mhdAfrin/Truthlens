export type Finding =
  "Supported" | "Contradicted" | "Mixed evidence" | "Insufficient evidence" | "Not assessed";
export type Claim = {
  id: number;
  text: string;
  finding: Finding;
  explanation: string;
  support: string[];
  against: string[];
  context: string;
  confidence: string;
};
export type VerificationReport = {
  extractionNote?: string;
  inputType?: string;
  originalUrl?: string;
  resolvedUrl?: string;
  id: string;
  title: string;
  content: string;
  date: string;
  sample: boolean;
  stored?: boolean;
  status?: string;
  stage?: string;
  reportAvailable?: boolean;
  accountOwned?: boolean;
  textMetrics?: { characters: number; words: number; paragraphs: number };
  claims: Claim[];
};
