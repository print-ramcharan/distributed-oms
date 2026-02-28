import { BrowserRouter as Router, Routes, Route, NavLink, useLocation } from 'react-router-dom'
import { LayoutDashboard, ShoppingCart, Zap, Inbox, Server, Layers } from 'lucide-react'
import Dashboard from './pages/Dashboard.jsx'
import Simulator from './pages/Simulator.jsx'
import ChaosHub from './pages/ChaosHub.jsx'
import DLQViewer from './pages/DLQViewer.jsx'
import InfraHub from './pages/InfraHub.jsx'
import ServicesControl from './pages/ServicesControl.jsx'

const NAV = [
  { path: '/', label: 'Overview', icon: LayoutDashboard },
  { path: '/simulator', label: 'Order Simulator', icon: ShoppingCart },
  { path: '/chaos', label: 'Chaos Hub', icon: Zap },
  { path: '/dlq', label: 'DLQ Viewer', icon: Inbox },
  { path: '/infra', label: 'Infrastructure', icon: Layers },
  { path: '/services', label: 'Services Control', icon: Server },
]

function ZentraLogo() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '20px 16px 16px', borderBottom: '1px solid var(--border)', marginBottom: '8px' }}>
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
        <defs>
          <radialGradient id="logoGrad" cx="30%" cy="30%" r="70%">
            <stop offset="0%" stopColor="#A78BFA" />
            <stop offset="40%" stopColor="#2B4CD4" />
            <stop offset="100%" stopColor="#00B8D9" />
          </radialGradient>
        </defs>
        <circle cx="16" cy="16" r="15" fill="url(#logoGrad)" />
        <text x="16" y="21" textAnchor="middle" fill="white" fontSize="16" fontWeight="bold" fontFamily="Arial">Z</text>
      </svg>
      <div>
        <div style={{ fontSize: '14px', fontWeight: 700, letterSpacing: '1px', color: 'var(--text-primary)' }}>ZENTRA</div>
        <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'" }}>Control Center</div>
      </div>
    </div>
  )
}

function Sidebar() {
  return (
    <aside style={{
      width: '220px', minHeight: '100vh', flexShrink: 0,
      background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)',
      display: 'flex', flexDirection: 'column', position: 'fixed', top: 0, left: 0, bottom: 0,
    }}>
      <ZentraLogo />

      <nav style={{ padding: '0 10px', flex: 1 }}>
        <div style={{ fontSize: '10px', fontFamily: "'Fira Code'", color: 'var(--text-muted)', letterSpacing: '1px', padding: '0 8px 8px', textTransform: 'uppercase' }}>
          Navigation
        </div>
        {NAV.map(({ path, label, icon: Icon }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            style={({ isActive }) => ({
              display: 'flex', alignItems: 'center', gap: '10px', padding: '9px 12px',
              borderRadius: '10px', marginBottom: '3px', textDecoration: 'none',
              fontSize: '13px', fontWeight: isActive ? 600 : 400, transition: 'all 0.15s',
              background: isActive ? 'rgba(0,184,217,0.1)' : 'transparent',
              color: isActive ? 'var(--zentra-cyan)' : 'var(--text-secondary)',
              border: isActive ? '1px solid rgba(0,184,217,0.2)' : '1px solid transparent',
            })}
          >
            <Icon size={15} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
          <span className="pulse-dot pulse-success" style={{ width: '7px', height: '7px' }} />
          <span style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--text-muted)' }}>UI: localhost:5173</span>
        </div>
        <div style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--text-muted)' }}>
          Gateway: :8080 · Kafka: :9092
        </div>
      </div>
    </aside>
  )
}

export default function App() {
  return (
    <Router>
      <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-primary)' }}>
        <Sidebar />
        <main style={{ marginLeft: '220px', flex: 1, minHeight: '100vh', overflow: 'auto' }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/simulator" element={<Simulator />} />
            <Route path="/chaos" element={<ChaosHub />} />
            <Route path="/dlq" element={<DLQViewer />} />
            <Route path="/infra" element={<InfraHub />} />
            <Route path="/services" element={<ServicesControl />} />
          </Routes>
        </main>
      </div>
    </Router>
  )
}
