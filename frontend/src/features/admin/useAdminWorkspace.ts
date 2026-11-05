import type { AdminData, PrivateSubmission, AdminUser } from "./adminTypes";
import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useAuth } from "../auth/AuthContext";
import { type Tab } from "./adminConfig";
import { emptyNews } from "./adminHelpers";
import { mutate } from "../../api/adminApi";
export function useAdminWorkspace() {
  const { user, loading, login, logout, refresh } = useAuth();
  const [tab, setTab] = useState<Tab>("overview"),
    [data, setData] = useState<AdminData | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [revision, setRevision] = useState(0),
    [query, setQuery] = useState(""),
    [page, setPage] = useState(0),
    [status, setStatus] = useState("");
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [detail, setDetail] = useState<PrivateSubmission | null>(null),
    [notice, setNotice] = useState(""),
    [news, setNews] = useState(emptyNews),
    [edit, setEdit] = useState<string | null>(null);
  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    let live = true;
    setError("");
    let params =
      tab === "users"
        ? `?q=${encodeURIComponent(query)}&page=${page}`
        : tab === "submissions"
          ? `?page=${page}&status=${status}`
          : tab === "audit"
            ? `?page=${page}`
            : "";
    api<AdminData>(`/api/admin/${tab}${params}`)
      .then((d) => {
        if (live) setData(d);
      })
      .catch((e) => {
        if (live) {
          setError(e.message);
          if (e.status === 401 || e.status === 403) void refresh();
        }
      });
    return () => {
      live = false;
    };
  }, [tab, user?.id, query, page, status, revision]);
  async function signIn(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await login(email, password);
      setPassword("");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function signOut() {
    setBusy(true);
    setError("");
    try {
      await logout();
      setData(null);
      setDetail(null);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function navigate(next: Tab) {
    setData(null);
    setTab(next);
    setPage(0);
    setQuery("");
    setStatus("");
    setDetail(null);
    setNotice("");
    setError("");
  }
  async function toggle(u: AdminUser) {
    setBusy(true);
    setError("");
    try {
      await mutate(`/api/admin/users/${u.id}`, "PATCH", { active: !u.active });
      setNotice(`${u.name}'s account is now ${u.active ? "inactive" : "active"}.`);
      setRevision((n) => n + 1);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function inspect(id: string) {
    setError("");
    try {
      setDetail(await api<PrivateSubmission>(`/api/admin/submissions/${id}`));
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function saveNews(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await mutate(`/api/admin/news${edit ? "/" + edit : ""}`, edit ? "PUT" : "POST", {
        ...news,
        publishedAt: new Date(news.publishedAt + ":00Z").toISOString(),
      });
      setNews(emptyNews());
      setEdit(null);
      setNotice("News item saved. Published items appear on the home page.");
      setRevision((n) => n + 1);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function exportData() {
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `truthlens-${tab}-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return {
    user,
    loading,
    tab,
    data,
    error,
    busy,
    query,
    page,
    status,
    email,
    password,
    detail,
    notice,
    news,
    edit,
    setTab,
    setData,
    setError,
    setBusy,
    setRevision,
    setQuery,
    setPage,
    setStatus,
    setEmail,
    setPassword,
    setDetail,
    setNotice,
    setNews,
    setEdit,
    signIn,
    signOut,
    navigate,
    toggle,
    inspect,
    saveNews,
    exportData,
  };
}
export type AdminWorkspaceState = ReturnType<typeof useAdminWorkspace>;
