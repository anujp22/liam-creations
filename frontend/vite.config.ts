import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Mirrors the production CloudFront path behaviours: the SPA is the default
    // origin, and these prefixes are routed to the backend. Keep the two in sync —
    // anything added here needs a matching CloudFront behaviour (see infra/DEPLOYMENT.md).
    proxy: {
      '/api': 'http://localhost:8080',
      // Uploaded product images are served by the backend from app.uploads.dir.
      '/uploads': 'http://localhost:8080',
    },
  },
})
