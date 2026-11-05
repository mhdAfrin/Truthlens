import type { CsrfToken } from "../types/api";
import { api } from "./client";
export async function postAuth(path: string, body?: URLSearchParams) {
  const csrf = await api<CsrfToken>("/api/auth/csrf");
  return api(path, {
    method: "POST",
    headers: {
      [csrf.headerName]: csrf.token,
      ...(body ? { "Content-Type": "application/x-www-form-urlencoded" } : {}),
    },
    body,
  });
}
