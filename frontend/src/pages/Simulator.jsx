import { useState } from 'react'
import { ShoppingCart, CreditCard, Package, Truck, Bell, CheckCircle, XCircle, Loader, AlertTriangle, ChevronRight } from 'lucide-react'
import { useSagaStore } from '../store.js'

const PRODUCTS = [
    { id: 'p1', name: 'MacBook Pro 16"', price: 249900, category: 'Electronics', stock: 12 },
    { id: 'p2', name: 'iPhone 16 Pro', price: 134900, category: 'Electronics', stock: 48 },
    { id: 'p3', name: 'Sony WH-1000XM5', price: 29990, category: 'Audio', stock: 3 },
    { id: 'p4', name: 'Samsung 4K Monitor', price: 54999, category: 'Display', stock: 7 },
]

const STEP_ICONS = { ShoppingCart, CreditCard, Package, Truck, Bell }

const STEP_COLORS = {
    pending: 'var(--text-muted)',
    processing: 'var(--zentra-blue)',
    completed: 'var(--success)',
    failed: 'var(--danger)',
    compensating: 'var(--warning)',
}

const STEP_BG = {
    pending: 'rgba(74,85,104,0.1)',
    processing: 'rgba(43,76,212,0.15)',
    completed: 'rgba(0,212,170,0.15)',
    failed: 'rgba(239,68,68,0.15)',
    compensating: 'rgba(245,158,11,0.15)',
}

function SagaNode({ step, index }) {
    const Icon = STEP_ICONS[step.icon]
    const color = STEP_COLORS[step.status]
    const bg = STEP_BG[step.status]
    const isProcessing = step.status === 'processing'
    const isFailed = step.status === 'failed'
    const isCompensating = step.status === 'compensating'

    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px', flex: 1 }}>
            <div
                className={isProcessing ? 'saga-node-active' : ''}
                style={{
                    width: '52px', height: '52px', borderRadius: '50%',
                    background: bg, border: `2px solid ${color}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color, position: 'relative', transition: 'all 0.3s ease',
                    boxShadow: step.status !== 'pending' ? `0 0 16px ${color}55` : 'none',
                }}
            >
                {isProcessing && (
                    <div style={{ position: 'absolute', inset: '-6px', borderRadius: '50%', border: `2px solid ${color}44` }} />
                )}
                {isFailed ? <XCircle size={22} color="var(--danger)" /> :
                    isCompensating ? <AlertTriangle size={22} color="var(--warning)" /> :
                        step.status === 'completed' ? <CheckCircle size={22} color="var(--success)" /> :
                            <Icon size={20} />}
            </div>
            <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '2px' }}>{step.label}</div>
                <span className={`badge badge-${step.status === 'completed' ? 'success' : step.status === 'failed' ? 'danger' : step.status === 'compensating' ? 'warning' : step.status === 'processing' ? 'info' : 'pending'}`}>
                    {step.status}
                </span>
            </div>
        </div>
    )
}

function ConnectorArrow({ fromStep, reversed }) {
    const isActive = fromStep.status === 'completed' || fromStep.status === 'compensating'
    const color = reversed ? 'var(--warning)' : 'var(--zentra-blue)'
    return (
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '32px', flex: 0.5 }}>
            {reversed ? (
                <ChevronRight size={16} color={color} style={{ transform: 'rotate(180deg)', opacity: isActive ? 1 : 0.2 }} />
            ) : (
                <ChevronRight size={16} color={color} style={{ opacity: isActive ? 1 : 0.2 }} />
            )}
        </div>
    )
}

function SagaVisualizer({ order }) {
    if (!order) return null
    const hasCompensation = order.steps.some(s => s.status === 'compensating' || s.status === 'failed')

    return (
        <div className="glass-card" style={{ padding: '24px', marginTop: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <div>
                    <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Saga Transaction Visualizer</h3>
                    <div style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)', marginTop: '2px' }}>{order.id}</div>
                </div>
                <span className={`badge badge-${order.status === 'CONFIRMED' ? 'success' : order.status === 'CANCELLED' ? 'danger' : order.status === 'COMPENSATING' ? 'warning' : 'info'}`}>
                    {order.status}
                </span>
            </div>

            {hasCompensation && (
                <div style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '10px', padding: '10px 14px', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle size={14} color="var(--danger)" />
                    <span style={{ fontSize: '12px', color: 'var(--warning)', fontFamily: "'Fira Code'" }}>Saga Compensation in progress — rolling back completed steps</span>
                </div>
            )}

            {/* Nodes + Connectors */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                {order.steps.map((step, idx) => (
                    <>
                        <SagaNode key={step.id} step={step} index={idx} />
                        {idx < order.steps.length - 1 && (
                            <ConnectorArrow key={`conn-${idx}`} fromStep={step} reversed={step.status === 'compensating' || step.status === 'failed'} />
                        )}
                    </>
                ))}
            </div>

            {/* Timeline */}
            <div style={{ marginTop: '20px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
                {order.steps.filter(s => s.status !== 'pending').map(step => (
                    <div key={step.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0', fontSize: '11px' }}>
                        <span style={{ color: 'var(--text-secondary)', fontFamily: "'Fira Code'" }}>{step.label}</span>
                        <span style={{ color: STEP_COLORS[step.status], fontFamily: "'Fira Code'" }}>
                            {step.completedAt ? step.completedAt.toLocaleTimeString() : step.startedAt ? `${step.startedAt.toLocaleTimeString()} — processing...` : ''}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default function Simulator() {
    const [selected, setSelected] = useState(null)
    const [scenario, setScenario] = useState('success')
    const { createOrder, simulateSaga, activeOrder, orders } = useSagaStore()

    const handleOrder = () => {
        if (!selected) return
        const orderId = createOrder(selected)
        if (scenario !== 'success') {
            // Trigger failure scenario after small delay
            setTimeout(() => {
                useSagaStore.setState((state) => ({
                    orders: state.orders.map(o =>
                        o.id === orderId ? { ...o, steps: o.steps.map(s => ({ ...s, status: 'pending' })) } : o
                    ),
                }))
                simulateSaga(orderId, scenario)
            }, 100)
        }
    }

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>Order Simulator</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                    Place an order and watch the Saga pattern execute in real-time across all microservices.
                </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.6fr', gap: '24px' }}>
                {/* Left: Product + Scenario Selector */}
                <div>
                    <div className="glass-card" style={{ padding: '20px', marginBottom: '16px' }}>
                        <h3 style={{ fontSize: '13px', fontWeight: 600, marginBottom: '14px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Select Product</h3>
                        {PRODUCTS.map(p => (
                            <div
                                key={p.id}
                                onClick={() => setSelected(p)}
                                style={{
                                    padding: '12px 14px', borderRadius: '12px', marginBottom: '8px', cursor: 'pointer',
                                    border: `1px solid ${selected?.id === p.id ? 'var(--zentra-cyan)' : 'var(--border)'}`,
                                    background: selected?.id === p.id ? 'rgba(0,184,217,0.07)' : 'rgba(255,255,255,0.02)',
                                    transition: 'all 0.15s',
                                }}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <div style={{ fontSize: '13px', fontWeight: 500 }}>{p.name}</div>
                                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>{p.category} · {p.stock} in stock</div>
                                    </div>
                                    <div style={{ fontFamily: "'Fira Code'", fontSize: '13px', color: 'var(--zentra-cyan)', fontWeight: 600 }}>
                                        ₹{p.price.toLocaleString()}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Scenario Selector */}
                    <div className="glass-card" style={{ padding: '20px', marginBottom: '16px' }}>
                        <h3 style={{ fontSize: '13px', fontWeight: 600, marginBottom: '14px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Test Scenario</h3>
                        {[
                            { id: 'success', label: 'Happy Path', desc: 'All steps succeed', color: 'var(--success)' },
                            { id: 'payment_failure', label: 'Payment Failure', desc: 'Saga compensates → Cancel', color: 'var(--danger)' },
                            { id: 'inventory_failure', label: 'Out of Stock', desc: 'Refund + Cancel', color: 'var(--warning)' },
                        ].map(s => (
                            <div
                                key={s.id}
                                onClick={() => setScenario(s.id)}
                                style={{
                                    padding: '10px 14px', borderRadius: '10px', marginBottom: '6px', cursor: 'pointer',
                                    border: `1px solid ${scenario === s.id ? s.color : 'var(--border)'}`,
                                    background: scenario === s.id ? `${s.color}11` : 'transparent',
                                    transition: 'all 0.15s',
                                }}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <div style={{ fontSize: '12px', fontWeight: 500, color: scenario === s.id ? s.color : 'var(--text-primary)' }}>{s.label}</div>
                                        <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{s.desc}</div>
                                    </div>
                                    {scenario === s.id && <CheckCircle size={14} color={s.color} />}
                                </div>
                            </div>
                        ))}
                    </div>

                    <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center', padding: '12px' }} onClick={handleOrder} disabled={!selected}>
                        <ShoppingCart size={15} />
                        Place Order{selected ? ` — ₹${selected.price.toLocaleString()}` : ''}
                    </button>
                </div>

                {/* Right: Saga Visualizer */}
                <div>
                    {activeOrder ? (
                        <SagaVisualizer order={orders.find(o => o.id === activeOrder.id) || activeOrder} />
                    ) : (
                        <div className="glass-card" style={{ padding: '48px 24px', textAlign: 'center' }}>
                            <ShoppingCart size={40} color="var(--text-muted)" style={{ margin: '0 auto 16px' }} />
                            <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Select a product and click "Place Order"<br />to watch the Saga execute in real-time</div>
                        </div>
                    )}

                    {/* Previous Orders */}
                    {orders.length > 1 && (
                        <div className="glass-card" style={{ padding: '16px', marginTop: '16px' }}>
                            <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", marginBottom: '10px' }}>Previous Orders</div>
                            {orders.slice(1, 5).map(o => (
                                <div key={o.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '5px 0', borderBottom: '1px solid var(--border)', fontSize: '11px' }}>
                                    <span style={{ fontFamily: "'Fira Code'", color: 'var(--zentra-cyan)' }}>{o.id}</span>
                                    <span className={`badge badge-${o.status === 'CONFIRMED' ? 'success' : o.status === 'CANCELLED' ? 'danger' : 'warning'}`}>{o.status}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
