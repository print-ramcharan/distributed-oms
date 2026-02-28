import { useState } from 'react'
import { Zap, Power, Copy, Wifi, WifiOff, Clock, AlertTriangle, CheckCircle } from 'lucide-react'
import { useSagaStore } from '../store.js'

function ChaosAction({ title, desc, buttonLabel, buttonColor, onTrigger, icon: Icon, lastResult }) {
    const [loading, setLoading] = useState(false)

    const handleClick = async () => {
        setLoading(true)
        await new Promise(r => setTimeout(r, 800))
        onTrigger()
        setLoading(false)
    }

    return (
        <div className="glass-card chaos-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                <div style={{ padding: '10px', borderRadius: '12px', background: `${buttonColor}18`, border: `1px solid ${buttonColor}33`, flexShrink: 0 }}>
                    <Icon size={18} color={buttonColor} />
                </div>
                <div style={{ flex: 1 }}>
                    <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{title}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{desc}</div>
                </div>
            </div>
            {lastResult && (
                <div style={{
                    fontFamily: "'Fira Code'", fontSize: '10px', color: lastResult.success ? 'var(--success)' : 'var(--danger)',
                    background: lastResult.success ? 'rgba(0,212,170,0.07)' : 'rgba(239,68,68,0.07)',
                    border: `1px solid ${lastResult.success ? 'rgba(0,212,170,0.2)' : 'rgba(239,68,68,0.2)'}`,
                    borderRadius: '8px', padding: '6px 10px', display: 'flex', aligItems: 'center', gap: '6px'
                }}>
                    {lastResult.success ? <CheckCircle size={11} /> : <AlertTriangle size={11} />}
                    {' '}{lastResult.message}
                </div>
            )}
            <button
                className={`btn btn-${buttonColor === 'var(--danger)' ? 'danger' : 'ghost'}`}
                style={{ justifyContent: 'center', border: `1px solid ${buttonColor}`, color: buttonColor }}
                onClick={handleClick}
                disabled={loading}
            >
                {loading ? <><Zap size={13} style={{ animation: 'spin 1s linear infinite' }} /> Running...</> : buttonLabel}
            </button>
        </div>
    )
}

function CircuitBreakerPanel({ services }) {
    return (
        <div className="glass-card" style={{ padding: '20px' }}>
            <h3 style={{ fontSize: '13px', fontWeight: 600, marginBottom: '14px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Circuit Breaker Status
            </h3>
            {services.map(s => {
                const isUp = s.status === 'UP'
                const cbState = s.circuitBreaker || (isUp ? 'CLOSED' : 'OPEN')
                return (
                    <div key={s.id} style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        padding: '8px 12px', borderRadius: '8px', background: 'rgba(255,255,255,0.02)',
                        border: '1px solid var(--border)', marginBottom: '6px'
                    }}>
                        <div style={{ display: 'flex', align: 'center', gap: '8px' }}>
                            <span className={`pulse-dot pulse-${isUp ? 'success' : 'danger'}`} />
                            <span style={{ fontSize: '12px', marginLeft: '8px' }}>{s.name}</span>
                        </div>
                        <span className={`badge badge-${cbState === 'CLOSED' ? 'success' : cbState === 'OPEN' ? 'danger' : 'warning'}`}>
                            {cbState}
                        </span>
                    </div>
                )
            })}
        </div>
    )
}

export default function ChaosHub() {
    const { services, toggleService } = useSagaStore()
    const [results, setResults] = useState({})

    const triggerResult = (key, result) => setResults(r => ({ ...r, [key]: result }))

    const killService = (id) => {
        toggleService(id)
        const s = services.find(s => s.id === id)
        const isKilling = s?.status === 'UP'
        triggerResult(id, { success: !isKilling, message: isKilling ? `${s?.name} killed! Circuit breaker: OPEN` : `${s?.name} restored! Circuit breaker: CLOSED` })
    }

    const injectDuplicate = () => {
        triggerResult('duplicate', { success: true, message: 'Injected 3 duplicate payment events. Idempotency keys blocked 2/3 duplicates.' })
    }

    const injectNetworkDelay = () => {
        triggerResult('delay', { success: true, message: 'Injected 5s delay on inventory-service. Retry mechanism triggered.' })
    }

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>
                    <span style={{ color: 'var(--danger)' }}>⚡</span> Chaos Engineering Hub
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                    Deliberately introduce failures to prove system resilience. This is exactly what FAANG SRE teams do in production.
                </p>
            </div>

            {/* Warning Banner */}
            <div style={{
                background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.3)', borderRadius: '12px',
                padding: '12px 16px', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '10px'
            }}>
                <AlertTriangle size={16} color="var(--warning)" />
                <span style={{ fontSize: '12px', color: 'var(--warning)', fontFamily: "'Fira Code'" }}>
                    Simulation mode — actions simulate chaos scenarios without affecting real infrastructure.
                </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '24px' }}>
                {/* Chaos Actions */}
                <div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", letterSpacing: '0.5px', marginBottom: '12px', textTransform: 'uppercase' }}>Chaos Actions</div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <ChaosAction
                            title="Kill Payment Service"
                            desc="Simulates a crash of the Payment Service. Opens the circuit breaker and triggers fallback responses."
                            buttonLabel={services.find(s => s.id === 'payment-service')?.status === 'DOWN' ? '⬆ Restore Service' : '⬇ Kill Service'}
                            buttonColor="var(--danger)"
                            icon={Power}
                            onTrigger={() => killService('payment-service')}
                            lastResult={results['payment-service']}
                        />
                        <ChaosAction
                            title="Kill Inventory Service"
                            desc="Brings down the Inventory service to test the Saga compensation (refund + cancel order)."
                            buttonLabel={services.find(s => s.id === 'inventory-service')?.status === 'DOWN' ? '⬆ Restore Service' : '⬇ Kill Service'}
                            buttonColor="var(--danger)"
                            icon={Power}
                            onTrigger={() => killService('inventory-service')}
                            lastResult={results['inventory-service']}
                        />
                        <ChaosAction
                            title="Inject Duplicate Payment"
                            desc="Sends 3 identical payment events. Proves Redis idempotency keys block duplicate charges."
                            buttonLabel="Inject Duplicate Events"
                            buttonColor="var(--warning)"
                            icon={Copy}
                            onTrigger={injectDuplicate}
                            lastResult={results['duplicate']}
                        />
                        <ChaosAction
                            title="Network Delay"
                            desc="Adds 5-second delay to inventory responses, triggering retry + exponential backoff."
                            buttonLabel="Inject 5s Delay"
                            buttonColor="var(--zentra-blue)"
                            icon={Clock}
                            onTrigger={injectNetworkDelay}
                            lastResult={results['delay']}
                        />
                    </div>
                </div>

                {/* Circuit Breaker Panel */}
                <CircuitBreakerPanel services={services} />
            </div>

            {/* Interview Cheat Sheet */}
            <div className="glass-card" style={{ padding: '20px', marginTop: '24px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>Interview Talking Points</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                    {[
                        { q: 'How do you prevent duplicate charges?', a: 'Redis idempotency keys with 24h TTL. Card is charged once regardless of network retries.' },
                        { q: 'What happens when Inventory crashes?', a: 'Circuit breaker opens, saga compensation refunds the payment, order is cancelled.' },
                        { q: 'How do you handle cascading failures?', a: 'Circuit breakers + bulkheads prevent cascading. Each service degrades independently.' },
                        { q: 'What is at-least-once delivery?', a: 'Kafka may retry. Outbox pattern + idempotent consumers = effectively-once processing.' },
                    ].map(({ q, a }) => (
                        <div key={q} style={{ background: 'rgba(43,76,212,0.05)', border: '1px solid rgba(43,76,212,0.15)', borderRadius: '10px', padding: '12px' }}>
                            <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--zentra-cyan)', marginBottom: '4px' }}>Q: {q}</div>
                            <div style={{ fontSize: '11px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>A: {a}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}
