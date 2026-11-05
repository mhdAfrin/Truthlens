import { useEffect, useState, type ReactNode } from "react";
import { ArrowRight, LockKeyhole } from "lucide-react";
import { go } from "../../lib/navigation";
import { api, ApiError } from "../../api/client";
import { useAuth } from "./AuthContext";

export function Protected({ admin = false, children }: { admin?: boolean; children: ReactNode }) {
  const { user, loading, refresh, error: authError } = useAuth();
  const [state, setState] = useState("checking"),
    [message, setMessage] = useState(""),
    [retry, setRetry] = useState(0);
  const accessKey = (user?.id || "anonymous") + ":" + (user?.role || "") + ":" + admin;
  useEffect(() => {
    let live = true;
    setState("checking");
    if (!user) return;
    api(admin ? "/api/admin/session" : "/api/account/profile")
      .then(() => {
        if (live) setState("allowed:" + accessKey);
      })
      .catch((e) => {
        if (!live) return;
        setState(e instanceof ApiError && e.status === 403 ? "denied" : "error");
        setMessage((e as Error).message);
        if (e instanceof ApiError && e.status === 401) void refresh();
      });
    return () => {
      live = false;
    };
  }, [user?.id, user?.role, admin, retry]);
  if (loading)
    return (
      <div className="auth-panel" role="status">
        Checking your session…
      </div>
    );
  if (!user)
    return (
      <div className="auth-panel">
        <LockKeyhole size={30} />
        <h1>Sign in to continue</h1>
        <p>{authError || "This area is available to signed-in, active accounts."}</p>
        <button className="button primary" onClick={() => go("login")}>
          Sign in <ArrowRight size={16} />
        </button>
        <button className="text-button" onClick={() => go("register")}>
          Create an account
        </button>
      </div>
    );
  if (state === "checking" || (state.startsWith("allowed:") && state !== "allowed:" + accessKey))
    return (
      <div className="auth-panel" role="status">
        Checking account access…
      </div>
    );
  if (state !== "allowed:" + accessKey)
    return (
      <div className="auth-panel" role="alert">
        <LockKeyhole size={30} />
        <h1>{state === "denied" ? "Administrator access required" : "Unable to confirm access"}</h1>
        <p>
          {state === "denied"
            ? "Your account does not have permission to open this area."
            : message}
        </p>
        {state === "error" && (
          <button className="button quiet" onClick={() => setRetry((n) => n + 1)}>
            Try again
          </button>
        )}
        <button className="button primary" onClick={() => go("account")}>
          My account
        </button>
      </div>
    );
  return <>{children}</>;
}
