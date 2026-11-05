import { ScanLine, Check, Minus, Scale, Search, ShieldCheck, ArrowRight } from "lucide-react";
import type { Finding } from "../types/report";
export function Brand() {
  return (
    <span className="brand">
      <span className="brand-icon">
        <ScanLine size={25} />
      </span>
      TruthLens<span className="brand-dot">.</span>
    </span>
  );
}
export function Badge({ finding }: { finding: Finding }) {
  const Icon =
    finding === "Supported"
      ? Check
      : finding === "Mixed evidence"
        ? Scale
        : finding === "Not assessed"
          ? Minus
          : Search;
  return (
    <span className={"badge " + finding.toLowerCase().replaceAll(" ", "-")}>
      <Icon size={13} />
      {finding}
    </span>
  );
}
export function SectionTitle({
  tag,
  title,
  description,
}: {
  tag: string;
  title: string;
  description: string;
}) {
  return (
    <div className="page-title">
      <span className="eyebrow">{tag}</span>
      <h1>{title}</h1>
      <p>{description}</p>
    </div>
  );
}
export function Empty({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: () => void;
}) {
  return (
    <div className="empty-state">
      <span>
        <Search size={28} />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action && (
        <button className="button primary" onClick={action}>
          Start a new check <ArrowRight size={16} />
        </button>
      )}
    </div>
  );
}
export function Note({ children }: { children: React.ReactNode }) {
  return (
    <div className="note">
      <ShieldCheck size={18} />
      <span>{children}</span>
    </div>
  );
}
