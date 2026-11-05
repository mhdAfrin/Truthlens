export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export async function api<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(path, {
      ...options,
      credentials: "same-origin",
      cache: "no-store",
      signal: controller.signal,
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok)
      throw new ApiError(body.message || "The request could not be completed.", response.status);
    return body as T;
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError("We couldn’t reach TruthLens. Please try again.", 0);
  } finally {
    clearTimeout(timer);
  }
}
