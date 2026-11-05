import { LayoutDashboard, Users, FileText, Newspaper, History, Database } from "lucide-react";

export const tabs = [
  ["overview", "Overview", LayoutDashboard],
  ["users", "People & access", Users],
  ["submissions", "Submissions", FileText],
  ["news", "News desk", Newspaper],
  ["audit", "Activity log", History],
  ["system", "Database & system", Database],
] as const;
export type Tab = (typeof tabs)[number][0];
