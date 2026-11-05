import { StoredReportPage } from "../features/history/StoredReportPage";
import { openStoredReport } from "../features/history/historyApi";
import { useAuth } from "../features/auth/AuthContext";
import { api } from "../api/client";
import { LoginPage } from "../features/auth/LoginPage";
import { AccountPage } from "../features/auth/AccountPage";
import { Protected } from "../features/auth/Protected";
import { useEffect, useState } from "react";
import { Shell } from "../components/Shell";
import { currentPage, go } from "../lib/navigation";
import { sampleReport } from "../data/sampleInvestigation";
import { type VerificationReport } from "../types/report";
import { VerifyPage } from "../pages/VerifyPage";
import { LibraryPage } from "../pages/LibraryPage";
import { ReportPage } from "../pages/ReportPage";
import { ClaimsPage } from "../pages/ClaimsPage";
import { SourcesPage } from "../pages/SourcesPage";
import { MethodPage } from "../pages/MethodPage";
import { HelpPage } from "../pages/HelpPage";

import { Registration } from "../features/registration/Registration";

export function PublicWorkspace() {
  const { user, refresh } = useAuth();
  const [page, setPage] = useState(currentPage),
    [report, setReport] = useState<VerificationReport>(sampleReport),
    [reports, setReports] = useState<VerificationReport[]>([sampleReport]),
    [saved, setSaved] = useState<string[]>([]),
    [reportId, setReportId] = useState(() => location.hash.split("/")[1] || "");
  useEffect(() => {
    const onHash = () => {
      setPage(currentPage());
      setReportId(location.hash.split("/")[1] || "");
      window.scrollTo({ top: 0, behavior: "instant" });
    };
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);
  useEffect(() => {
    const latest = sessionStorage.getItem("truthlens-latest-verification");
    if (!latest) return;
    api<VerificationReport>("/api/verifications/" + encodeURIComponent(latest))
      .then((item) => setReports([item, sampleReport]))
      .catch(() => {});
  }, [page]);
  useEffect(() => {
    setReports([sampleReport]);
    setReport(sampleReport);
    setSaved([]);
  }, [user?.id]);
  function openReport(r: VerificationReport) {
    if (r.stored) {
      openStoredReport(r.id);
      return;
    }
    setReport(r);
    go("report");
  }
  if (page === "admin") {
    location.replace("/admin");
    return null;
  }
  return (
    <Shell page={page}>
      {page === "verify" && <VerifyPage openReport={openReport} />}{" "}
      {page === "library" && (
        <Protected>
          <LibraryPage />
        </Protected>
      )}{" "}
      {page === "report" && reportId && <StoredReportPage id={reportId} />}
      {page === "report" && !reportId && (
        <ReportPage
          report={report}
          saved={saved.includes(report.id)}
          toggleSaved={async () => {
            if (!user) {
              go("login");
              return;
            }
            try {
              await api("/api/account/profile");
              setSaved((old) =>
                old.includes(report.id)
                  ? old.filter((id) => id !== report.id)
                  : [...old, report.id],
              );
            } catch {
              await refresh();
              go("account");
            }
          }}
        />
      )}{" "}
      {page === "claims" && <ClaimsPage reports={reports} openReport={openReport} />}{" "}
      {page === "sources" && <SourcesPage reports={reports} />} {page === "method" && <MethodPage />}{" "}
      {page === "help" && <HelpPage />} {page === "login" && <LoginPage />}
      {page === "account" && (
        <Protected>
          <AccountPage />
        </Protected>
      )}
      {page === "register" && <Registration onBack={() => go("verify")} />}
    </Shell>
  );
}
