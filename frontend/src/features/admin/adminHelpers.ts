export const date = (v: string) => new Date(v).toLocaleString();
export const emptyNews = () => ({
  title: "",
  summary: "",
  publisher: "",
  sourceUrl: "",
  publishedAt: new Date().toISOString().slice(0, 16),
  visible: false,
  breaking: false,
});
