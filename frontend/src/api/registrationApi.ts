import type { Fields } from "../features/registration/validation";
export async function registerAccount(fields: Fields, signal: AbortSignal) {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...fields,
      name: fields.name.trim(),
      email: fields.email.trim().toLowerCase(),
    }),
    signal: signal,
  });
  const body = await response.json().catch(() => null);

  return { ok: response.ok, body };
}
