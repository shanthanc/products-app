import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true, // fail if 3000 is taken instead of silently moving to another port
    proxy: { '/api': 'http://localhost:8080' },
  },
})
