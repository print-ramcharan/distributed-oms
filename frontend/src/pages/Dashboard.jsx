import { useEffect } from 'react'
import { Activity, TrendingUp, Zap, Clock, CheckCircle, AlertTriangle, BarChart2, GitBranch } from 'lucide-react'
import { useMetricsStore, useSagaStore } from '../store.js'

function StatCard({ label, value, unit, icon: Icon, color, trend }) {
    return (
        <div className="glass-card" style={{ padding: '20px', position: 'relative', overflow: 'hidden' }}>
            <div style={{
                position: 'absolute', top: 0, right: 0, width: '80px', height: '80px',
                background: `radial-gradient(circle at top right, ${color}22, transparent 70%)`,
                borderRadius: '0 16px 0 0'
            }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", letterSpacing: '0.5px', textTransform: 'uppercase', marginBottom: '8px' }}>{label}</div>
                    <div className="stat-number" style={{ fontSize: '28px', color }}>
                        {typeof value === 'number' ? value.toFixed(value < 10 ? 1 : 0) : value}
                        <span style={{ fontSize: '13px', color: 'var(--text-muted)', marginLeft: '4px', fontWeight: 400 }}>{unit}</span>
                    </div>
                    {trend && <div style={{ fontSize: '11px', color: 'var(--success)', marginTop: '4px' }}>{trend}</div>}
                </div>
                <div style={{ padding: '10px', borderRadius: '12px', background: `${color}18`, border: `1px solid ${color}33` }}>
                    <Icon size={18} color={color} />
                </div>
            </div>
        </div>
    )
}

function ServiceRow({ service }) {
    const isUp = service.status === 'UP';
    return (
        <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '10px 16px', borderRadius: '10px', background: 'rgba(255,255,255,0.02)',
            border: '1px solid var(--border)', marginBottom: '6px'
        }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span className={`pulse-dot pulse-${isUp ? 'success' : 'danger'}`} />
                <span style={{ fontSize: '13px', color: 'var(--text-primary)' }}>{service.name}</span>
            </div>
            <div style={{ display: 'flex', align: 'center', gap: '12px' }}>
                <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--text-muted)' }}>:{service.port}</span>
                <span className={`badge badge-${isUp ? 'success' : 'danger'}`}>{service.status}</span>
                <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)', minWidth: '50px', textAlign: 'right' }}>{service.latency}ms</span>
            </div>
        </div>
    )
}

function RecentOrders({ orders }) {
    const statusColor = { PENDING: 'warning', PROCESSING: 'info', CONFIRMED: 'success', COMPENSATING: 'warning', CANCELLED: 'danger' };
    return (
        <div className="glass-card" style={{ padding: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>Recent Orders</h3>
                <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--text-muted)' }}>
                    {orders.length} transactions
                </span>
            </div>
            {orders.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)', fontSize: '13px' }}>
                    No orders yet — go to Order Simulator!
                </div>
            ) : (
                orders.slice(0, 8).map(o => (
                    <div key={o.id} style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        padding: '8px 0', borderBottom: '1px solid var(--border)'
                    }}>
                        <div>
                            <div style={{ fontFamily: "'Fira Code'", fontSize: '12px', color: 'var(--zentra-cyan)' }}>{o.id}</div>
                            <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{o.product}</div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                            <div style={{ fontFamily: "'Fira Code'", fontSize: '13px', color: 'var(--text-primary)', marginBottom: '2px' }}>₹{o.amount.toLocaleString()}</div>
                            <span className={`badge badge-${statusColor[o.status] || 'pending'}`}>{o.status}</span>
                        </div>
                    </div>
                ))
            )}
        </div>
    )
}

export default function Dashboard() {
    const { ordersPerMin, successRate, avgLatency, kafkaLag, activeTransactions, errorRate, tick } = useMetricsStore()
    const { services, orders } = useSagaStore()

    useEffect(() => {
        const interval = setInterval(tick, 2000)
        return () => clearInterval(interval)
    }, [tick])

    return (
        <div style={{ padding: '28px' }}>
            {/* Header */}
            <div style={{ marginBottom: '28px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '4px' }}>
                    System Overview
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px', fontFamily: "'Fira Code'" }}>
                    Zentra Distributed OMS — Real-time&nbsp;
                    <span style={{ color: 'var(--success)' }}>•&nbsp;Live</span>
                </p>
            </div>

            {/* Stat Cards Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>
                <StatCard label="Orders / Min" value={ordersPerMin} unit="req/min" icon={TrendingUp} color="var(--zentra-cyan)" trend="↑ Steady load" />
                <StatCard label="Success Rate" value={successRate} unit="%" icon={CheckCircle} color="var(--success)" />
                <StatCard label="Avg Latency (P99)" value={avgLatency} unit="ms" icon={Clock} color="var(--zentra-purple)" />
                <StatCard label="Kafka Lag" value={kafkaLag} unit="msgs" icon={Activity} color={kafkaLag > 20 ? 'var(--warning)' : 'var(--zentra-blue)'} />
                <StatCard label="Active Sagas" value={activeTransactions} unit="txns" icon={GitBranch} color="var(--zentra-blue)" />
                <StatCard label="Error Rate" value={errorRate} unit="%" icon={AlertTriangle} color={errorRate > 1 ? 'var(--danger)' : 'var(--warning)'} />
            </div>

            {/* Lower Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                {/* Service Health */}
                <div className="glass-card" style={{ padding: '20px' }}>
                    <h3 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '14px', color: 'var(--text-primary)' }}>
                        Service Health
                    </h3>
                    {services.map(s => <ServiceRow key={s.id} service={s} />)}
                </div>

                {/* Recent Orders */}
                <RecentOrders orders={orders} />
            </div>
        </div>
    )
}
