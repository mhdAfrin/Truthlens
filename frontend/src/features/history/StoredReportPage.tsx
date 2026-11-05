import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useAuth } from "../auth/AuthContext";
import { ReportPage } from "../../pages/ReportPage";
import type { VerificationReport } from "../../types/report";
import { go } from "../../lib/navigation";
export function StoredReportPage({ id }: { id: string }) {
  const { user, refresh } = useAuth();
  const [result, setResult] = useState<{
      key: string;
      report?: VerificationReport;
      error?: string;
    } | null>(null),
    [retry, setRetry] = useState(0);
  const key = user?.id + ":" + id;
  useEffect(() => {
    let live = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    setResult(null);
    const load = () =>
      api<VerificationReport>("/api/verifications/" + encodeURIComponent(id))
        .then((report) => {
          if (!live) return;
          setResult({ key, report });
          if (report.status === "PROCESSING") timer = setTimeout(load, 1000);
        })
        .catch(async (e) => {
          if (!live) return;
          setResult({ key, error: e.message });
          if (e.status === 401) await refresh();
        });
    void load();
    return () => {
      live = false;
      if (timer) clearTimeout(timer);
    };
  }, [key, retry]);
  if (!result || result.key !== key)
    return (
      <div className="history-loading" role="status">
        Loading your report…
      </div>
    );
  if (!result.report)
    return (
      <div className="auth-panel" role="alert">
        <h1>Unable to open this report</h1>
        <p>{result.error}</p>
        <button className="button primary" onClick={() => go("verify")}>
          New verification
        </button>
        <button className="text-button" onClick={() => setRetry((n) => n + 1)}>
          Try again
        </button>
      </div>
    );
  return <ReportPage report={result.report} saved={false} toggleSaved={() => {}} />;
}
