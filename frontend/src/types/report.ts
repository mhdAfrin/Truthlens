export type Finding =
  "Supported" | "Contradicted" | "Mixed evidence" | "Mixed" | "Insufficient evidence" | "Not assessed";
export type EvidenceSource = {
  title: string;
  url: string;
  snippet: string;
  sourceType: string;
  relationship: string;
  publisher: string;
  publicationDate: string;
  quote?: string;
  sourceTypeReason?: string;
  independenceNote?: string;
};

export type Claim = {
  id: number;
  claimType?: string;
  originalPassage?: string;
  startOffset?: number;
  endOffset?: number;
  searchStatus?: string;
  searchError?: string;
  evidence?: EvidenceSource[];
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
  error?: string;
  evidenceMethod?: string;
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
