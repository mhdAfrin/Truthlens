import { defineConfig } from "vite";
export default defineConfig({
  server: {
    host: "127.0.0.1",
    proxy: { "/api": process.env.TRUTHLENS_API_URL || "http://127.0.0.1:8093" },
  },
});
