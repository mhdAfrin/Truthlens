// Start the website and API together. No extra runner package is needed.
import { spawn, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const windows = process.platform === "win32";
const port = Number(process.env.TRUTHLENS_WEB_PORT || 5173);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid website port");
const children = [];
let stopping = false;

function stop(code = 0) {
  if (stopping) return;
  stopping = true;
  for (const child of children) {
    if (!child.pid) continue;
    if (windows) {
      // These IDs belong only to the processes started below.
      spawnSync("taskkill", ["/PID", String(child.pid), "/T", "/F"], { stdio: "ignore" });
    } else {
      try { process.kill(-child.pid, "SIGTERM"); } catch { /* Already stopped. */ }
    }
  }
  process.exit(code);
}

function start(command, args, cwd) {
  const child = spawn(command, args, { cwd, stdio: "inherit", detached: !windows });
  children.push(child);
  child.on("error", (error) => { console.error(error.message); stop(1); });
  child.on("exit", (code) => { if (!stopping) stop(code || 1); });
}

process.on("SIGINT", () => stop());
process.on("SIGTERM", () => stop());

if (windows) {
  start("powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "backend/scripts/start-api.ps1", "-Mode", "Preview"], root);
  start("cmd.exe", ["/d", "/s", "/c", `npm run dev --prefix frontend -- --strictPort --port ${port}`], root);
} else {
  start("mvn", ["spring-boot:test-run", "-Dspring-boot.run.profiles=preview"], path.join(root, "backend"));
  start("npm", ["run", "dev", "--prefix", "frontend", "--", "--strictPort", "--port", String(port)], root);
}
