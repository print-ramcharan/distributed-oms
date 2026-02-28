import { useEffect, useState, useCallback } from 'react'
import { Activity, TrendingUp, Zap, Clock, CheckCircle, AlertTriangle, GitBranch, Database } from 'lucide-react'
import {
    fetchAllServiceHealth,
    fetchPrometheusMetrics,
    fetchOrders,
    fetchAllServiceMetrics,
    SERVICE_DEFS,
} from '../api.js'

function StatCard({ label, value, unit, icon: Icon, color, subtitle }) {
    const displayVal = value === null || value === undefined ? '—' : typeof value === 'number' ? value.toFixed(value < 10 ? 1 : 0) : value
    return (
        <div className="glass-card" style={{ padding: '20px', position: 'relative', overflow: 'hidden' }}>
            <div style={{
                position: 'absolute', top: 0, right: 0, width: '80px', height: '80px',
                background: `radial-gradient(circle at top right, ${color}22, transparent 70%)`, borderRadius: '0 16px 0 0'
            }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '8px' }}>{label}</div>
                    <div className="stat-number" style={{ fontSize: '28px', color }}>
                        {displayVal}
                        <span style={{ fontSize: '13px', color: 'var(--text-muted)', marginLeft: '4px', fontWeight: 400 }}>{unit}</span>
                    </div>
                    {subtitle && <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>{subtitle}</div>}
                </div>
                <div style={{ padding: '10px', borderRadius: '12px', background: `${color}18`, border: `1px solid ${color}33` }}>
                    <Icon size={18} color={color} />
                </div>
            </div>
        </div>
    )
}

function ServiceHealthTable({ services, metrics }) {
    const metricMap = Object.fromEntries((metrics || []).map(m => [m.id, m]))
    return (
        <div className="glass-card" style={{ padding: '20px' }}>
            <h3 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '14px' }}>Service Health & JVM Metrics</h3>
            <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                    <thead>
                        <tr style={{ borderBottom: '1px solid var(--border)' }}>
                            {['Service', 'Status', 'Port', 'CPU %', 'Heap MB', 'Threads', 'HTTP Reqs'].map(h => (
                                <th key={h} style={{ padding: '6px 10px', textAlign: 'left', color: 'var(--text-muted)', fontFamily: "'Fira Code'", fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{h}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {services.map(svc => {
                            const m = metricMap[svc.id]
                            const isUp = svc.status === 'UP'
                            return (
                                <tr key={svc.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
                                    <td style={{ padding: '8px 10px' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                            <span className={`pulse-dot pulse-${isUp ? 'success' : 'danger'}`} />
                                            <span style={{ marginLeft: '8px' }}>{svc.name}</span>
                                        </div>
                                    </td>
                                    <td style={{ padding: '8px 10px' }}>
                                        <span className={`badge badge-${isUp ? 'success' : 'danger'}`}>{svc.status}</span>
                                    </td>
                                    <td style={{ padding: '8px 10px', fontFamily: "'Fira Code'", color: 'var(--text-muted)' }}>{svc.port}</td>
                                    <td style={{ padding: '8px 10px', fontFamily: "'Fira Code'", color: m?.cpuPercent > 70 ? 'var(--danger)' : 'var(--zentra-cyan)' }}>
                                        {m?.cpuPercent != null ? `${m.cpuPercent}%` : '—'}
                                    </td>
                                    <td style={{ padding: '8px 10px', fontFamily: "'Fira Code'", color: 'var(--text-secondary)' }}>
                                        {m?.memUsedMb != null ? `${m.memUsedMb} / ${m.memMaxMb}` : '—'}
                                    </td>
                                    <td style={{ padding: '8px 10px', fontFamily: "'Fira Code'", color: 'var(--text-secondary)' }}>
                                        {m?.threads ?? '—'}
                                    </td>
                                    <td style={{ padding: '8px 10px', fontFamily: "'Fira Code'", color: 'var(--success)' }}>
                                        {m?.httpRequests != null ? m.httpRequests.toLocaleString() : '—'}
                                    </td>
                                </tr>
                            )
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

function RecentOrdersTable({ orders }) {
    const statusColor = { PENDING: 'warning', PROCESSING: 'info', CONFIRMED: 'success', COMPENSATING: 'warning', CANCELLED: 'danger', FAILED: 'danger' }
    return (
        <div className="glass-card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Recent Orders</h3>
                <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--text-muted)' }}>{orders.length} orders</span>
            </div>
            {orders.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--text-muted)', fontSize: '12px' }}>
                    Backend not connected — no orders yet
                </div>
            ) : (
                orders.slice(0, 10).map(o => (
                    <div key={o.orderId || o.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                        <div>
                            <div style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)' }}>{(o.orderId || o.id || '').toString().substring(0, 8).toUpperCase()}</div>
                            <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '1px' }}>{o.customerEmail || 'N/A'}</div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                            <div style={{ fontFamily: "'Fira Code'", fontSize: '12px', marginBottom: '2px' }}>
                                {o.totalAmount != null ? `₹${o.totalAmount.toLocaleString()}` : ''}
                            </div>
                            <span className={`badge badge-${statusColor[o.status] || 'pending'}`}>{o.status}</span>
                        </div>
                    </div>
                ))
            )}
        </div>
    )
}

export default function Dashboard() {
    const [services, setServices] = useState(SERVICE_DEFS.map(s => ({ ...s, status: 'CHECKING' })))
    const [metrics, setMetrics] = useState([])
    const [prom, setProm] = useState({})
    const [orders, setOrders] = useState([])
    const [lastUpdated, setLastUpdated] = useState(null)
    const [backendOnline, setBackendOnline] = useState(null) // null = unknown

    const refresh = useCallback(async () => {
        const [healthData, metricsData, promData, ordersData] = await Promise.all([
            fetchAllServiceHealth(),
            fetchAllServiceMetrics(),
            fetchPrometheusMetrics(),
            fetchOrders(20),
        ])
        setServices(healthData)
        setMetrics(metricsData.filter(Boolean))
        setProm(promData)
        setOrders(ordersData)
        setLastUpdated(new Date())
        setBackendOnline(healthData.some(s => s.status === 'UP'))
    }, [])

    useEffect(() => {
        refresh()
        const interval = setInterval(refresh, 5000)
        return () => clearInterval(interval)
    }, [refresh])

    const upCount = services.filter(s => s.status === 'UP').length
    const totalCount = services.length

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div>
                    <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>System Overview</h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '13px', fontFamily: "'Fira Code'" }}>
                        Zentra Distributed OMS ·{' '}
                        <span style={{ color: backendOnline === null ? 'var(--text-muted)' : backendOnline ? 'var(--success)' : 'var(--danger)' }}>
                            {backendOnline === null ? '⠷ Connecting...' : backendOnline ? `● Live — ${upCount}/${totalCount} services UP` : '● Backend Offline'}
                        </span>
                    </p>
                </div>
                {lastUpdated && (
                    <div style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--text-muted)' }}>
                        Updated {lastUpdated.toLocaleTimeString()}
                    </div>
                )}
            </div>

            {/* Top stats from Prometheus */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '20px' }}>
                <StatCard label="HTTP Req Rate" value={prom.httpRate} unit="req/s" icon={TrendingUp} color="var(--zentra-cyan)" subtitle="From Prometheus" />
                <StatCard label="P99 Latency" value={prom.p99LatencyMs} unit="ms" icon={Clock} color="var(--zentra-purple)" subtitle="From Prometheus" />
                <StatCard label="Error Rate" value={prom.errorRate != null ? +(prom.errorRate).toFixed(3) : null} unit="err/s" icon={AlertTriangle} color={prom.errorRate > 0.1 ? 'var(--danger)' : 'var(--warning)'} subtitle="5xx responses" />
                <StatCard label="Kafka Lag" value={prom.kafkaLag != null ? Math.round(prom.kafkaLag) : null} unit="msgs" icon={Activity} color={prom.kafkaLag > 50 ? 'var(--warning)' : 'var(--zentra-blue)'} subtitle="Consumer lag total" />
                <StatCard label="Services Up" value={`${upCount}/${totalCount}`} unit="" icon={CheckCircle} color={upCount === totalCount ? 'var(--success)' : 'var(--danger)'} subtitle="Live actuator health" />
                <StatCard label="Orders (last 20)" value={orders.length} unit="" icon={GitBranch} color="var(--zentra-blue)" subtitle="Real DB records" />
            </div>

            {/* Lower grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '3fr 2fr', gap: '20px' }}>
                <ServiceHealthTable services={services} metrics={metrics} />
                <RecentOrdersTable orders={orders} />
            </div>

            {!backendOnline && backendOnline !== null && (
                <div style={{ marginTop: '20px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '12px', padding: '14px 18px' }}>
                    <div style={{ fontSize: '12px', color: 'var(--warning)', fontFamily: "'Fira Code'" }}>
                        ⚠ Backend services are offline. Run <code style={{ color: 'var(--zentra-cyan)' }}>docker compose up -d</code> in <code style={{ color: 'var(--zentra-cyan)' }}>backend/</code> then start Spring Boot services to see live data.
                    </div>
                </div>
            )}
        </div>
    )
}
