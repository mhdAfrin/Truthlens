import { AuthProvider } from "./features/auth/AuthContext";
import { AdminWorkspace } from "./features/admin/AdminWorkspace";
import { PublicWorkspace } from "./app/PublicWorkspace";
import "./styles/app.css";
import "./styles/original-workspace.css";
import "./styles/integration.css";
import "./styles/admin.css";
export default function App() {
  const admin = location.pathname.replace(/\/$/, "") === "/admin";
  return <AuthProvider>{admin ? <AdminWorkspace /> : <PublicWorkspace />}</AuthProvider>;
}
