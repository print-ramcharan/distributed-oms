import { useEffect, useState, useCallback } from 'react'
import { Activity, TrendingUp, Zap, Clock, CheckCircle, AlertTriangle, GitBranch } from 'lucide-react'
import {
    fetchAllServiceHealth,
    fetchPrometheusMetrics,
    fetchOrders,
    fetchAllServiceMetrics,
    SERVICE_DEFS,
} from '../api.js'

function StatCard({ label, value, unit, icon: Icon, colorClass, bgClass, textClass, subtitle }) {
    const displayVal = value === null || value === undefined ? '—' : typeof value === 'number' ? value.toFixed(value < 10 ? 1 : 0) : value
    return (
        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm relative overflow-hidden">
            <div className={`absolute top-0 right-0 w-20 h-20 rounded-bl-full opacity-20 ${bgClass}`} />
            <div className="flex justify-between items-start relative z-10">
                <div>
                    <div className="text-[10px] font-mono font-semibold text-gray-500 uppercase tracking-widest mb-1.5">{label}</div>
                    <div className={`text-2xl font-bold font-mono ${textClass}`}>
                        {displayVal}
                        {unit && <span className="text-xs text-gray-400 ml-1 font-sans font-normal">{unit}</span>}
                    </div>
                    {subtitle && <div className="text-[10px] text-gray-400 mt-1.5">{subtitle}</div>}
                </div>
                <div className={`p-2.5 rounded-lg border ${bgClass} ${colorClass} bg-opacity-10 border-opacity-20`}>
                    <Icon size={18} className={colorClass} />
                </div>
            </div>
        </div>
    )
}

function ServiceHealthTable({ services, metrics }) {
    const metricMap = Object.fromEntries((metrics || []).map(m => [m.id, m]))
    return (
        <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Service Health & JVM Metrics</h3>
            <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="border-b border-gray-100">
                            {['Service', 'Status', 'Port', 'CPU %', 'Heap MB', 'Threads', 'HTTP Reqs'].map(h => (
                                <th key={h} className="pb-3 text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-wider">{h}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className="text-sm">
                        {services.map(svc => {
                            const m = metricMap[svc.id]
                            const isUp = svc.status === 'UP'
                            return (
                                <tr key={svc.id} className="border-b border-gray-50 last:border-0 hover:bg-gray-50/50 transition-colors">
                                    <td className="py-3 pr-4 font-medium text-gray-900">
                                        <div className="flex items-center gap-2.5">
                                            <span className={`w-2 h-2 rounded-full ${isUp ? 'bg-emerald-500' : 'bg-red-500'}`} />
                                            {svc.name}
                                        </div>
                                    </td>
                                    <td className="py-3 pr-4">
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium font-mono ${isUp ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-600/20' : 'bg-red-50 text-red-700 ring-1 ring-red-600/20'}`}>
                                            {svc.status}
                                        </span>
                                    </td>
                                    <td className="py-3 pr-4 font-mono text-xs text-gray-500">{svc.port}</td>
                                    <td className={`py-3 pr-4 font-mono text-xs ${m?.cpuPercent > 70 ? 'text-red-600 font-bold' : 'text-blue-600'}`}>
                                        {m?.cpuPercent != null ? `${m.cpuPercent}%` : '—'}
                                    </td>
                                    <td className="py-3 pr-4 font-mono text-xs text-gray-600">
                                        {m?.memUsedMb != null ? `${m.memUsedMb} / ${m.memMaxMb}` : '—'}
                                    </td>
                                    <td className="py-3 pr-4 font-mono text-xs text-gray-600">
                                        {m?.threads ?? '—'}
                                    </td>
                                    <td className="py-3 pr-4 font-mono text-xs text-emerald-600">
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
    const statusConfig = { 
        PENDING: 'bg-gray-100 text-gray-600 ring-gray-500/10', 
        PROCESSING: 'bg-blue-50 text-blue-700 ring-blue-600/20', 
        CONFIRMED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', 
        COMPENSATING: 'bg-orange-50 text-orange-700 ring-orange-600/20', 
        CANCELLED: 'bg-red-50 text-red-700 ring-red-600/20', 
        FAILED: 'bg-red-50 text-red-700 ring-red-600/20' 
    }
    return (
        <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
            <div className="flex justify-between items-center mb-4">
                <h3 className="text-sm font-semibold text-gray-900">Recent Orders</h3>
                <span className="font-mono text-[10px] text-gray-400 uppercase tracking-widest">{orders.length} orders</span>
            </div>
            {orders.length === 0 ? (
                <div className="text-center py-10 text-sm text-gray-500">
                    Backend not connected — no orders yet
                </div>
            ) : (
                <div className="divide-y divide-gray-100">
                    {orders.slice(0, 10).map(o => (
                        <div key={o.orderId || o.id} className="py-3 flex justify-between items-center hover:bg-gray-50/50 transition-colors -mx-2 px-2 rounded-md">
                            <div>
                                <div className="font-mono text-xs font-semibold text-blue-600">{(o.orderId || o.id || '').toString().substring(0, 8).toUpperCase()}</div>
                                <div className="text-xs text-gray-500 mt-0.5">{o.customerEmail || 'N/A'}</div>
                            </div>
                            <div className="text-right flex flex-col items-end gap-1">
                                <div className="font-mono text-xs font-medium text-gray-900">
                                    {o.totalAmount != null ? `₹${o.totalAmount.toLocaleString()}` : ''}
                                </div>
                                <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium font-mono ring-1 ring-inset ${statusConfig[o.status] || statusConfig.PENDING}`}>
                                    {o.status}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
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
    const [backendOnline, setBackendOnline] = useState(null)

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
        <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
            <div className="flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-semibold text-gray-900 tracking-tight">System Overview</h1>
                    <p className="text-sm text-gray-500 mt-1 flex items-center gap-2 font-mono">
                        Zentra Distributed OMS 
                        <span className="text-gray-300">|</span>
                        {backendOnline === null ? (
                            <span className="text-gray-400">⠷ Connecting...</span>
                        ) : backendOnline ? (
                            <span className="text-emerald-600 flex items-center gap-1.5">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                                Live — {upCount}/{totalCount} services UP
                            </span>
                        ) : (
                            <span className="text-red-500 flex items-center gap-1.5">
                                <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                                Backend Offline
                            </span>
                        )}
                    </p>
                </div>
                {lastUpdated && (
                    <div className="font-mono text-xs text-gray-400">
                        Updated {lastUpdated.toLocaleTimeString()}
                    </div>
                )}
            </div>

            {/* Top stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                <StatCard label="HTTP Req Rate" value={prom.httpRate} unit="req/s" icon={TrendingUp} textClass="text-blue-600" colorClass="text-blue-600" bgClass="bg-blue-500" subtitle="From Prometheus" />
                <StatCard label="P99 Latency" value={prom.p99LatencyMs} unit="ms" icon={Clock} textClass="text-purple-600" colorClass="text-purple-600" bgClass="bg-purple-500" subtitle="From Prometheus" />
                <StatCard label="Error Rate" value={prom.errorRate != null ? +(prom.errorRate).toFixed(3) : null} unit="err/s" icon={AlertTriangle} textClass={prom.errorRate > 0.1 ? 'text-red-600' : 'text-orange-500'} colorClass={prom.errorRate > 0.1 ? 'text-red-600' : 'text-orange-500'} bgClass={prom.errorRate > 0.1 ? 'bg-red-500' : 'bg-orange-500'} subtitle="5xx responses" />
                <StatCard label="Kafka Lag" value={prom.kafkaLag != null ? Math.round(prom.kafkaLag) : null} unit="msgs" icon={Activity} textClass={prom.kafkaLag > 50 ? 'text-orange-500' : 'text-indigo-600'} colorClass={prom.kafkaLag > 50 ? 'text-orange-500' : 'text-indigo-600'} bgClass={prom.kafkaLag > 50 ? 'bg-orange-500' : 'bg-indigo-500'} subtitle="Consumer lag total" />
                <StatCard label="Services Up" value={`${upCount}/${totalCount}`} unit="" icon={CheckCircle} textClass={upCount === totalCount ? 'text-emerald-600' : 'text-red-600'} colorClass={upCount === totalCount ? 'text-emerald-600' : 'text-red-600'} bgClass={upCount === totalCount ? 'bg-emerald-500' : 'bg-red-500'} subtitle="Live actuator health" />
                <StatCard label="Orders (last 20)" value={orders.length} unit="" icon={GitBranch} textClass="text-blue-600" colorClass="text-blue-600" bgClass="bg-blue-500" subtitle="Real DB records" />
            </div>

            {/* Lower grid */}
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                <div className="xl:col-span-2">
                    <ServiceHealthTable services={services} metrics={metrics} />
                </div>
                <div className="xl:col-span-1">
                    <RecentOrdersTable orders={orders} />
                </div>
            </div>

            {!backendOnline && backendOnline !== null && (
                <div className="bg-orange-50 border border-orange-200 rounded-lg p-4 flex gap-3">
                    <AlertTriangle className="text-orange-500 shrink-0" size={20} />
                    <div className="text-sm text-orange-800">
                        <span className="font-semibold block mb-1">Backend services are offline.</span>
                        Run <code className="bg-orange-100 px-1 py-0.5 rounded text-orange-900 font-mono text-xs">docker compose up -d</code> in <code className="bg-orange-100 px-1 py-0.5 rounded text-orange-900 font-mono text-xs">backend/</code> then start Spring Boot services to see live data.
                    </div>
                </div>
            )}
        </div>
    )
}
