import type { Account } from "../../types/account";
export type AdminUser = Account & { createdAt: string };
export type AdminSubmission = {
  id: string;
  title: string;
  createdAt: string;
  status: string;
  inputType: string;
  ownership: string;
  ownerId: string | null;
};
export type PrivateSubmission = {
  id: string;
  title: string;
  content: string;
  originalUrl: string | null;
  status: string;
  ownerId: string | null;
};
export type NewsItem = {
  id: string;
  title: string;
  summary: string;
  publisher: string;
  sourceUrl: string;
  publishedAt: string;
  visible: boolean;
  breaking: boolean;
  origin: string;
};
export type OverviewData = {
  users: number;
  activeUsers: number;
  submissions: number;
  processing: number;
  awaiting: number;
  publishedNews: number;
};
export type RecordsData = { items: (AdminUser | AdminSubmission)[]; total: number };
export type NewsData = { items: NewsItem[] };
export type AuditData = { items: Record<string, string>[]; total: number };
export type SystemData = {
  database: string;
  databaseStatus: string;
  sessions: string;
  passwordStorage: string;
  verificationEngine: string;
  tables: { name: string; rows: number }[];
  migrations: Record<string, string | boolean>[];
};
export type AdminData = OverviewData | RecordsData | NewsData | AuditData | SystemData;
