import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const ngrokEnabled = process.env.VITE_NGROK === '1'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    host: true,
    allowedHosts: ['localhost', '.ngrok-free.app', '.ngrok-free.dev', '.ngrok.io', '.ngrok.app'],
    hmr: ngrokEnabled
      ? {
          clientPort: 443,
          protocol: 'wss',
        }
      : undefined,
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
