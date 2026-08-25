import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
        // 流式（SSE）场景必须禁用反代 buffering，否则 proxy 会把整个响应
        // 攒完再一次性交给前端，浏览器端看起来就是"非流式"
        configure: (proxy) => {
          proxy.on("proxyRes", (proxyRes) => {
            const ct = String(proxyRes.headers["content-type"] || "");
            if (ct.toLowerCase().includes("text/event-stream")) {
              proxyRes.headers["x-accel-buffering"] = "no";
              proxyRes.headers["cache-control"] = "no-cache, no-transform";
              proxyRes.headers["connection"] = "keep-alive";
              delete proxyRes.headers["content-length"];
            }
          });
        }
      },
      "/files": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true
      },
      "/ws": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true,
        ws: true
      }
    }
  }
});
