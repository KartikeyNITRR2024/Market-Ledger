import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      // Dev proxy: avoids CORS. Backend defaults to Spring Boot on :8080
      proxy: { '/api': { target: env.VITE_API_PROXY || 'http://localhost:1997', changeOrigin: true } },
    },
  }
})
