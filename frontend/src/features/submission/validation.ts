export function validateArticleText(text: string, limit: number) {
  if (!text.trim()) return "Paste an article or copied text before starting verification.";
  if (Array.from(text).length > limit)
    return `This article is too long. Keep it to ${limit.toLocaleString()} Unicode characters.`;
  return "";
}
export function validateArticleUrl(value: string) {
  const raw = value.trim();
  if (!/^https?:\/\//i.test(raw) || !raw || raw.length > 2048 || /[\s\\\x00-\x1f]/.test(raw))
    return "Enter a valid HTTP or HTTPS article URL (up to 2,048 characters).";
  try {
    const parsed = new URL(raw);
    if (
      !["http:", "https:"].includes(parsed.protocol) ||
      !parsed.hostname ||
      parsed.username ||
      parsed.password
    )
      throw new Error();
  } catch {
    return "Enter a valid HTTP or HTTPS article URL, such as https://news.example.com/article.";
  }
  return "";
}
