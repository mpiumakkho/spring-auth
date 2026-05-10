import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy /bff/** to the web-api BFF (which forwards to core-api).
    // Frontend NEVER hits core-api directly — the BFF holds the API key
    // and translates the JWT cookie into Authorization headers.
    proxy: {
      "/bff": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
