import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import {
  LayoutDashboard, ShoppingCart, Zap, Inbox, Activity,
  GitBranch, Globe, ChevronRight
} from 'lucide-react'
import Dashboard from './pages/Dashboard.jsx'
import Simulator from './pages/Simulator.jsx'
import ChaosHub from './pages/ChaosHub.jsx'
import DLQViewer from './pages/DLQViewer.jsx'

const NAV = [
  { to: '/', label: 'Overview', Icon: LayoutDashboard },
  { to: '/simulator', label: 'Order Simulator', Icon: ShoppingCart },
  { to: '/chaos', label: 'Chaos Hub', Icon: Zap },
  { to: '/dlq', label: 'DLQ Viewer', Icon: Inbox },
]

function ZentraLogo() {
  return (
    <div style={{ padding: '20px 16px 16px', borderBottom: '1px solid var(--border)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {/* SVG Z sphere icon inspired by the logo */}
        <svg width="34" height="34" viewBox="0 0 34 34" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <radialGradient id="sphere-grad" cx="40%" cy="35%" r="65%">
              <stop offset="0%" stopColor="#C060FF" />
              <stop offset="50%" stopColor="#2B4CD4" />
              <stop offset="100%" stopColor="#00B8D9" />
            </radialGradient>
          </defs>
          <circle cx="17" cy="17" r="16" fill="url(#sphere-grad)" />
          <path d="M10 11.5 L24 11.5 L13 17.5 L24 22.5 L10 22.5" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        </svg>
        <div>
          <div style={{ fontFamily: "'Fira Sans', sans-serif", fontWeight: 700, fontSize: '16px', letterSpacing: '1.5px', color: 'var(--text-primary)' }}>ZENTRA</div>
          <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code', monospace", letterSpacing: '0.5px' }}>Control Center</div>
        </div>
      </div>
    </div>
  )
}

function Sidebar() {
  const linkStyle = (isActive) => ({
    display: 'flex', alignItems: 'center', gap: '10px',
    padding: '9px 16px', borderRadius: '10px', margin: '2px 8px',
    fontSize: '13px', fontWeight: isActive ? 600 : 400,
    color: isActive ? 'var(--zentra-cyan)' : 'var(--text-secondary)',
    background: isActive ? 'rgba(0,184,217,0.08)' : 'transparent',
    border: isActive ? '1px solid rgba(0,184,217,0.2)' : '1px solid transparent',
    textDecoration: 'none', cursor: 'pointer',
    transition: 'all 0.15s ease',
  })

  return (
    <div className="sidebar">
      <ZentraLogo />
      <nav style={{ flex: 1, padding: '12px 0' }}>
        <div style={{ padding: '8px 16px 6px', fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", letterSpacing: '1px', textTransform: 'uppercase' }}>Navigation</div>
        {NAV.map(({ to, label, Icon }) => (
          <NavLink key={to} to={to} end={to === '/'} style={({ isActive }) => linkStyle(isActive)}>
            <Icon size={15} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Bottom status */}
      <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
          <span className="pulse-dot pulse-success" />
          <span style={{ fontSize: '11px', color: 'var(--text-secondary)', fontFamily: "'Fira Code'" }}>All Systems UP</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Activity size={11} color="var(--text-muted)" />
          <span style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'" }}>Kafka: Connected</span>
        </div>
      </div>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <div style={{ display: 'flex', minHeight: '100vh' }}>
        <Sidebar />
        <div className="main-content" style={{ flex: 1 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/simulator" element={<Simulator />} />
            <Route path="/chaos" element={<ChaosHub />} />
            <Route path="/dlq" element={<DLQViewer />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  )
}
