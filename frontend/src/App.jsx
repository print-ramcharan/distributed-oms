import { BrowserRouter as Router, Routes, Route, NavLink } from 'react-router-dom'
import { LayoutDashboard, ShoppingCart, Zap, Inbox, Server, Layers, Activity } from 'lucide-react'
import { Toaster } from 'sonner'
import { LoadTestProvider } from './context/LoadTestContext.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Simulator from './pages/Simulator.jsx'
import ChaosHub from './pages/ChaosHub.jsx'
import DLQViewer from './pages/DLQViewer.jsx'
import InfraHub from './pages/InfraHub.jsx'
import ServicesControl from './pages/ServicesControl.jsx'
import LoadTester from './pages/LoadTester.jsx'

const NAV = [
  { path: '/', label: 'Overview', icon: LayoutDashboard },
  { path: '/simulator', label: 'Order Simulator', icon: ShoppingCart },
  { path: '/load-tester', label: 'Load Tester', icon: Activity },
  { path: '/chaos', label: 'Chaos Hub', icon: Zap },
  { path: '/dlq', label: 'DLQ Viewer', icon: Inbox },
  { path: '/infra', label: 'Infrastructure', icon: Layers },
  { path: '/services', label: 'Services Control', icon: Server },
]

function ZentraLogo() {
  return (
    <div className="flex items-center gap-3 p-5 border-b border-gray-200 bg-white">
      <svg width="28" height="28" viewBox="0 0 32 32" fill="none" className="shrink-0">
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
        <div className="text-[13px] font-bold tracking-widest text-gray-900 leading-tight">ZENTRA</div>
        <div className="text-[10px] text-gray-500 font-mono tracking-wide">Control Center</div>
      </div>
    </div>
  )
}

function Sidebar() {
  return (
    <aside className="fixed inset-y-0 left-0 w-60 bg-white border-r border-gray-200 flex flex-col z-50">
      <ZentraLogo />

      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <div className="px-3 pb-2 text-[10px] font-mono text-gray-400 tracking-widest uppercase">
          Navigation
        </div>
        {NAV.map(({ path, label, icon: Icon }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-md text-[13px] font-medium transition-all duration-150 ` +
              (isActive
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900')
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-gray-200 bg-gray-50/50">
        <div className="flex items-center gap-2 mb-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          <span className="font-mono text-[10px] text-gray-500">UI: localhost:5173</span>
        </div>
        <div className="font-mono text-[10px] text-gray-400">
          Gateway: :8080 · Kafka: :9092
        </div>
      </div>
    </aside>
  )
}

export default function App() {
  return (
    <LoadTestProvider>
      <Router>
        <div className="flex min-h-screen bg-gray-50/50">
          <Toaster position="top-right" richColors />
          <Sidebar />
          <main className="ml-60 flex-1 min-h-screen overflow-auto">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/simulator" element={<Simulator />} />
              <Route path="/load-tester" element={<LoadTester />} />
              <Route path="/chaos" element={<ChaosHub />} />
              <Route path="/dlq" element={<DLQViewer />} />
              <Route path="/infra" element={<InfraHub />} />
              <Route path="/services" element={<ServicesControl />} />
            </Routes>
          </main>
        </div>
      </Router>
    </LoadTestProvider>
  )
}
