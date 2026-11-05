export type Page =
  | "verify"
  | "library"
  | "report"
  | "claims"
  | "sources"
  | "method"
  | "admin"
  | "register"
  | "help"
  | "login"
  | "account";
export function currentPage(): Page {
  const value = location.hash.slice(1).split("/")[0];
  return [
    "verify",
    "library",
    "report",
    "claims",
    "sources",
    "method",
    "admin",
    "register",
    "help",
    "login",
    "account",
  ].includes(value)
    ? (value as Page)
    : "verify";
}
export function go(page: Page) {
  location.hash = page;
  window.scrollTo({ top: 0, behavior: "instant" });
}
