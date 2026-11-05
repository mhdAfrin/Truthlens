export const statusLabels: Record<string, string> = {
  AWAITING_VERIFICATION: "Awaiting verification",
  PROCESSING: "Processing",
  COMPLETED: "Completed",
  FAILED: "Failed",
};
export type HistoryItem = {
  id: string;
  title: string;
  createdAt: string;
  status: string;
  reportAvailable: boolean;
};
export type HistoryResult = {
  items: HistoryItem[];
  page: number;
  totalPages: number;
  totalElements: number;
};
export function openStoredReport(id: string) {
  location.hash = "report/" + encodeURIComponent(id);
  window.scrollTo({ top: 0, behavior: "instant" });
}
