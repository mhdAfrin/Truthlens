import type { CsrfToken } from "../types/api";
import { api } from "./client";
export async function mutate(path: string, method: string, body: unknown) {
  const c = await api<CsrfToken>("/api/auth/csrf");
  return api(path, {
    method,
    headers: { "Content-Type": "application/json", [c.headerName]: c.token },
    body: JSON.stringify(body),
  });
}
