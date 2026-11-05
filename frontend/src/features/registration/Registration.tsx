import { registerAccount } from "../../api/registrationApi";
import { go } from "../../lib/navigation";
import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  Eye,
  EyeOff,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";
import { validateRegistration, type Fields } from "./validation";
export function Registration({ onBack }: { onBack: () => void }) {
  const [f, setF] = useState<Fields>({ name: "", email: "", password: "" }),
    [errors, setErrors] = useState<Partial<Fields>>({}),
    [show, setShow] = useState(false),
    [busy, setBusy] = useState(false),
    [failure, setFailure] = useState(""),
    [success, setSuccess] = useState(false);
  const first = useRef<HTMLInputElement>(null),
    status = useRef<HTMLDivElement>(null);
  useEffect(() => {
    first.current?.focus();
  }, []);
  useEffect(() => {
    if (success) status.current?.focus();
  }, [success]);
  const checks = [
    ["8 or more characters", f.password.length >= 8],
    ["A letter and a number", /[A-Za-z]/.test(f.password) && /[0-9]/.test(f.password)],
    [
      "Within the password length limit",
      !!f.password && new TextEncoder().encode(f.password).length <= 72,
    ],
  ] as const;
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    setFailure("");
    const next = validateRegistration(f);
    setErrors(next);
    if (Object.keys(next).length) {
      document.getElementById("reg-" + Object.keys(next)[0])?.focus();
      return;
    }
    setBusy(true);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    try {
      const { ok, body } = await registerAccount(f, controller.signal);
      if (!ok) {
        setErrors(body?.errors ?? {});
        setFailure(body?.message ?? "We couldn’t create your account. Please try again.");
        return;
      }
      setF({ name: "", email: "", password: "" });
      setSuccess(true);
    } catch {
      setFailure(
        "We couldn’t reach TruthLens. Check your connection and try again. Your account has not been confirmed.",
      );
    } finally {
      clearTimeout(timer);
      setBusy(false);
    }
  }
  return (
    <section className="registration">
      <button className="text-button back-link" onClick={onBack}>
        <ArrowLeft size={16} />
        Back to verification
      </button>
      <div className="registration-layout">
        <aside className="account-story">
          <div className="eyebrow">A FRESH PERSPECTIVE STARTS HERE</div>
          <h1>
            A calmer corner
            <br />
            of the <em>internet.</em>
          </h1>
          <p>
            For the questions worth asking.
            <br />
            For the stories worth understanding.
          </p>
          <div className="account-editorial" aria-hidden="true">
            <span>THE CLEARER PICTURE</span>
            <div className="editorial-sheet">
              <small>START WITH CURIOSITY</small>
              <h3>
                Read it.
                <br />
                Question it.
                <br />
                <em>Understand it.</em>
              </h3>
              <div />
              <div />
              <div />
            </div>
            <span className="editorial-stamp">
              <ShieldCheck size={19} />
              YOUR JUDGMENT
              <br />
              COMES FIRST
            </span>
          </div>
          <div className="account-values">
            <div>
              <CheckCircle2 size={17} />
              <p>
                <strong>Evidence in perspective</strong>Understand the reasoning behind the finding.
              </p>
            </div>
            <div>
              <LockKeyhole size={17} />
              <p>
                <strong>A thoughtful foundation</strong>Your password is securely hashed by our API.
              </p>
            </div>
          </div>
          <small>
            Register and sign in to keep your reports together in your personal history.
          </small>
        </aside>
        <div className="registration-card card">
          {success ? (
            <div className="registration-success" ref={status} tabIndex={-1} role="status">
              <span className="success-icon">
                <CheckCircle2 size={38} />
              </span>
              <div className="eyebrow">A CLEAR START</div>
              <h2>You’re all set.</h2>
              <p>Your TruthLens account was created successfully.</p>
              <p className="muted">You can now sign in with your email and password.</p>
              <button className="button primary" onClick={() => go("login")}>
                Sign in to TruthLens <ArrowRight size={17} />
              </button>
              <button
                className="text-button"
                onClick={() => {
                  setSuccess(false);
                  setTimeout(() => first.current?.focus(), 0);
                }}
              >
                Create another account
              </button>
            </div>
          ) : (
            <>
              <span className="account-symbol">
                <LockKeyhole size={22} />
              </span>
              <div className="eyebrow">WELCOME TO TRUTHLENS</div>
              <h2>Create your account</h2>
              <p className="registration-intro">
                Already registered? Use Sign in at the top of the page.
              </p>
              <form onSubmit={submit} noValidate aria-busy={busy}>
                {(["name", "email", "password"] as const).map((key) => (
                  <div className="registration-field" key={key}>
                    <label htmlFor={"reg-" + key}>
                      {key === "name"
                        ? "Full name"
                        : key === "email"
                          ? "Email address"
                          : "Password"}{" "}
                      <span aria-hidden="true">*</span>
                    </label>
                    <div className="password-input">
                      <input
                        ref={key === "name" ? first : undefined}
                        id={"reg-" + key}
                        name={key}
                        required
                        type={
                          key === "password"
                            ? show
                              ? "text"
                              : "password"
                            : key === "email"
                              ? "email"
                              : "text"
                        }
                        autoComplete={key === "password" ? "new-password" : key}
                        placeholder={
                          key === "name"
                            ? "Your full name"
                            : key === "email"
                              ? "you@example.com"
                              : "Create a strong password"
                        }
                        value={f[key]}
                        disabled={busy}
                        aria-invalid={!!errors[key]}
                        aria-describedby={
                          errors[key]
                            ? "error-" + key
                            : key === "password"
                              ? "password-guide"
                              : undefined
                        }
                        onChange={(e) => {
                          setF({ ...f, [key]: e.target.value });
                          setErrors({ ...errors, [key]: undefined });
                          setFailure("");
                        }}
                      />
                      {key === "password" && (
                        <button
                          className="password-toggle"
                          type="button"
                          disabled={busy}
                          aria-label={show ? "Hide password" : "Show password"}
                          aria-pressed={show}
                          onClick={() => setShow(!show)}
                        >
                          {show ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                      )}
                    </div>
                    {errors[key] && (
                      <p className="field-error" id={"error-" + key}>
                        {errors[key]}
                      </p>
                    )}
                  </div>
                ))}
                <div id="password-guide" className="password-guide">
                  {checks.map(([label, ok]) => (
                    <span key={label} className={ok ? "met" : ""}>
                      <Check size={13} />
                      {label}
                    </span>
                  ))}
                </div>
                {failure && (
                  <div className="registration-error" role="alert">
                    {failure}
                  </div>
                )}
                <button className="button primary register-submit" disabled={busy} type="submit">
                  {busy ? "Creating your account…" : "Create account"}
                  {!busy && <ArrowRight size={17} />}
                </button>
                <p className="account-security">
                  <LockKeyhole size={13} />
                  Your password is securely hashed on our server.
                </p>
              </form>
              <div className="account-footnote">
                <ShieldCheck size={17} />
                <span>
                  Only the essentials. Your name, email, and a secure password. All fields are
                  required.
                </span>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
