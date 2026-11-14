import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Any request to /api is forwarded to your Spring Boot server (no CORS setup needed in dev).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
