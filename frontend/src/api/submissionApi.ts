import type { CsrfToken } from "../types/api";
import { api } from "./client";
export type SubmissionAccepted = {
  extractionNote?: string;
  inputType?: string;
  originalUrl?: string;
  resolvedUrl?: string;
  id: string;
  requestId: string;
  status: string;
  message: string;
  reportAvailable: boolean;
};
export async function submitVerification(
  content: string,
  accountRequired = false,
): Promise<SubmissionAccepted> {
  const csrf = await api<CsrfToken>("/api/auth/csrf");
  return api("/api/verifications", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(accountRequired ? { "X-TruthLens-Account": "required" } : {}),
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ content }),
  });
}
export async function submitArticleUrl(
  url: string,
  accountRequired = false,
): Promise<SubmissionAccepted> {
  const csrf = await api<CsrfToken>("/api/auth/csrf");
  return api("/api/verifications/url", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(accountRequired ? { "X-TruthLens-Account": "required" } : {}),
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ url: url.trim() }),
  });
}
