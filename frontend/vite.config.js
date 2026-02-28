import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Vite dev proxy — avoids CORS by routing all /proxy/* through Vite to the real services
const SERVICE_PORTS = {
  order: 8081,
  payment: 8082,
  inventory: 8083,
  notification: 8084,
  saga: 8085,
  fulfillment: 8086,
  gateway: 8080,
}

const proxy = {}
for (const [name, port] of Object.entries(SERVICE_PORTS)) {
  proxy[`/proxy/${name}`] = {
    target: `http://localhost:${port}`,
    changeOrigin: true,
    rewrite: (path) => path.replace(new RegExp(`^/proxy/${name}`), ''),
  }
}

// Expose Prometheus and Kafka UI through proxy too
proxy['/proxy/prometheus'] = {
  target: 'http://localhost:9090',
  changeOrigin: true,
  rewrite: (path) => path.replace(/^\/proxy\/prometheus/, ''),
}

proxy['/proxy/kafka-ui'] = {
  target: 'http://localhost:8090',
  changeOrigin: true,
  rewrite: (path) => path.replace(/^\/proxy\/kafka-ui/, ''),
}

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy,
  },
})
