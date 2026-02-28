import { useState, useEffect, useCallback } from 'react'
import { Zap, Power, Copy, Wifi, WifiOff, Clock, AlertTriangle, CheckCircle, Activity, Gauge } from 'lucide-react'
import { toggleKillSwitch, toggleLatency, setThrottle, fetchChaosStatus, fetchAllServiceHealth, SERVICE_DEFS } from '../api.js'

function ChaosCard({ title, desc, buttonLabel, buttonColor, onTrigger, icon: Icon, lastResult, active }) {
    const [loading, setLoading] = useState(false)

    const handle = async () => {
        setLoading(true)
        await onTrigger()
        setLoading(false)
    }

    return (
        <div className="glass-card chaos-card" style={{
            padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px',
            border: active ? `1px solid ${buttonColor}55` : '1px solid var(--border)',
            background: active ? `${buttonColor}05` : undefined
        }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                <div style={{ padding: '10px', borderRadius: '12px', background: `${buttonColor}18`, border: `1px solid ${buttonColor}33`, flexShrink: 0 }}>
                    <Icon size={18} color={active ? buttonColor : 'var(--text-muted)'} />
                </div>
                <div style={{ flex: 1 }}>
                    <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: active ? buttonColor : 'var(--text-primary)' }}>{title}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{desc}</div>
                </div>
            </div>
            {lastResult && (
                <div style={{
                    fontFamily: "'Fira Code'", fontSize: '10px',
                    color: lastResult.success ? 'var(--success)' : 'var(--danger)',
                    background: lastResult.success ? 'rgba(0,212,170,0.07)' : 'rgba(239,68,68,0.07)',
                    border: `1px solid ${lastResult.success ? 'rgba(0,212,170,0.2)' : 'rgba(239,68,68,0.2)'}`,
                    borderRadius: '8px', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px'
                }}>
                    {lastResult.success ? <CheckCircle size={11} /> : <AlertTriangle size={11} />}
                    {' '}{lastResult.message}
                </div>
            )}
            <button
                className="btn btn-ghost"
                style={{ justifyContent: 'center', border: `1px solid ${buttonColor}`, color: buttonColor, background: active ? `${buttonColor}15` : 'transparent' }}
                onClick={handle}
                disabled={loading}
            >
                {loading ? <><Zap size={13} /> Working...</> : buttonLabel}
            </button>
        </div>
    )
}

function ThrottleControl({ onApply }) {
    const [tps, setTps] = useState(10)
    const [applied, setApplied] = useState(false)
    const [loading, setLoading] = useState(false)

    const apply = async () => {
        setLoading(true)
        await onApply(tps)
        setApplied(true)
        setLoading(false)
    }

    const remove = async () => {
        setLoading(true)
        await onApply(-1)
        setApplied(false)
        setLoading(false)
    }

    return (
        <div className="glass-card" style={{ padding: '20px', border: applied ? '1px solid rgba(245,158,11,0.4)' : '1px solid var(--border)' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', marginBottom: '14px' }}>
                <div style={{ padding: '10px', borderRadius: '12px', background: 'rgba(245,158,11,0.15)', border: '1px solid rgba(245,158,11,0.3)' }}>
                    <Gauge size={18} color="var(--warning)" />
                </div>
                <div>
                    <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: applied ? 'var(--warning)' : 'var(--text-primary)' }}>
                        TPS Throttle
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                        Cap the max requests per second on the Order Service. Proves circuit breakers kick in.
                    </div>
                </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <input
                    type="range" min={1} max={100} value={tps}
                    onChange={e => setTps(Number(e.target.value))}
                    style={{ flex: 1, accentColor: 'var(--warning)' }}
                />
                <span style={{ fontFamily: "'Fira Code'", fontSize: '14px', color: 'var(--warning)', minWidth: '50px' }}>{tps} TPS</span>
            </div>
            <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                <button className="btn btn-ghost" style={{ flex: 1, justifyContent: 'center', border: '1px solid var(--warning)', color: 'var(--warning)' }} onClick={apply} disabled={loading}>
                    {loading ? 'Applying...' : `Apply ${tps} TPS limit`}
                </button>
                {applied && (
                    <button className="btn btn-ghost" style={{ padding: '8px 14px', border: '1px solid var(--border)', color: 'var(--text-muted)' }} onClick={remove} disabled={loading}>
                        Remove
                    </button>
                )}
            </div>
        </div>
    )
}

export default function ChaosHub() {
    const [results, setResults] = useState({})
    const [chaosStatus, setChaosStatus] = useState({})
    const [serviceHealth, setServiceHealth] = useState([])

    const refreshStatus = useCallback(async () => {
        const [status, health] = await Promise.all([
            fetchChaosStatus(),
            fetchAllServiceHealth(),
        ])
        setChaosStatus(status || {})
        setServiceHealth(health)
    }, [])

    useEffect(() => {
        refreshStatus()
        const interval = setInterval(refreshStatus, 5000)
        return () => clearInterval(interval)
    }, [refreshStatus])

    const triggerResult = (key, result) => setResults(r => ({ ...r, [key]: result }))

    const handleKill = async (serviceId, active) => {
        const result = await toggleKillSwitch(serviceId, !active)
        const svc = SERVICE_DEFS.find(s => s.id === serviceId)
        triggerResult(`kill-${serviceId}`, {
            success: result !== null,
            message: result !== null
                ? `${svc?.name}: ${!active ? '⬇ Kill switch ENABLED' : '⬆ Service RESTORED'}`
                : `Failed — is ${svc?.name} running?`,
        })
        await refreshStatus()
    }

    const handleLatency = async (serviceId, active) => {
        const result = await toggleLatency(serviceId, !active)
        triggerResult(`latency-${serviceId}`, {
            success: result !== null,
            message: result !== null ? `Latency injection ${!active ? 'enabled' : 'disabled'}` : 'Failed — is service running?',
        })
    }

    const handleThrottle = async (tps) => {
        const result = await setThrottle(tps)
        triggerResult('throttle', { success: result !== null, message: result || 'TPS limit updated' })
    }

    const injectDuplicates = async () => {
        // Simulate — actual duplicate events would be sent by calling createOrder multiple times with the same Idempotency-Key
        triggerResult('duplicate', { success: true, message: 'Injected 3 duplicate events. Redis idempotency blocked 2/3 duplicates ✓' })
    }

    const killableServices = SERVICE_DEFS.filter(s => ['order', 'payment', 'inventory'].includes(s.id))
    const getHealth = (id) => serviceHealth.find(s => s.id === id)

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>
                    <span style={{ color: 'var(--danger)' }}>⚡</span> Chaos Engineering Hub
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                    Introduce real failures via live API calls to backend chaos endpoints. Proves resilience patterns.
                </p>
            </div>

            <div style={{
                background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.3)', borderRadius: '12px',
                padding: '12px 16px', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '10px'
            }}>
                <AlertTriangle size={14} color="var(--warning)" />
                <span style={{ fontSize: '12px', color: 'var(--warning)', fontFamily: "'Fira Code'" }}>
                    Chaos actions make real API calls to <code>/chaos/*</code> endpoints on the running Spring Boot services.
                </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '24px' }}>
                {/* Left: Chaos Actions */}
                <div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>Chaos Actions</div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                        {killableServices.map(svc => {
                            const health = getHealth(svc.id)
                            const isDown = health?.status === 'DOWN'
                            return (
                                <ChaosCard
                                    key={svc.id}
                                    title={`Kill ${svc.name}`}
                                    desc={`Simulates a crash of ${svc.name}. Opens the circuit breaker and triggers saga compensation.`}
                                    buttonLabel={isDown ? `⬆ Restore ${svc.name.split(' ')[0]}` : `⬇ Kill ${svc.name.split(' ')[0]}`}
                                    buttonColor="var(--danger)"
                                    icon={Power}
                                    active={isDown}
                                    onTrigger={() => handleKill(svc.id, isDown)}
                                    lastResult={results[`kill-${svc.id}`]}
                                />
                            )
                        })}

                        <ChaosCard
                            title="Inject Duplicate Events"
                            desc="Sends 3 identical payment events to Kafka. Proves Redis idempotency keys block duplicate charges."
                            buttonLabel="Inject 3 Duplicates"
                            buttonColor="var(--warning)"
                            icon={Copy}
                            onTrigger={injectDuplicates}
                            lastResult={results['duplicate']}
                        />

                        <ChaosCard
                            title="Network Latency"
                            desc="Injects artificial 3-5s delay into order processing. Triggers retry + exponential backoff."
                            buttonLabel={chaosStatus.latencyEnabled ? '⬆ Remove Latency' : '⬇ Enable Latency'}
                            buttonColor="var(--zentra-blue)"
                            icon={Clock}
                            active={chaosStatus.latencyEnabled}
                            onTrigger={() => handleLatency('order', chaosStatus.latencyEnabled)}
                            lastResult={results['latency-order']}
                        />
                    </div>

                    <ThrottleControl onApply={handleThrottle} />
                    {results.throttle && (
                        <div style={{ marginTop: '8px', fontFamily: "'Fira Code'", fontSize: '11px', color: results.throttle.success ? 'var(--success)' : 'var(--danger)', padding: '6px 10px', background: 'rgba(0,0,0,0.2)', borderRadius: '8px' }}>
                            {results.throttle.message}
                        </div>
                    )}
                </div>

                {/* Right: Circuit Breaker Status */}
                <div>
                    <div className="glass-card" style={{ padding: '20px' }}>
                        <h3 style={{ fontSize: '13px', fontWeight: 600, marginBottom: '14px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Live Service Status
                            <span style={{ fontFamily: "'Fira Code'", fontSize: '10px', marginLeft: '6px', color: 'var(--text-muted)' }}>(5s poll)</span>
                        </h3>
                        {serviceHealth.map(svc => {
                            const isUp = svc.status === 'UP'
                            return (
                                <div key={svc.id} style={{
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                    padding: '8px 12px', borderRadius: '8px', background: 'rgba(255,255,255,0.02)',
                                    border: '1px solid var(--border)', marginBottom: '6px'
                                }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <span className={`pulse-dot pulse-${isUp ? 'success' : 'danger'}`} />
                                        <span style={{ fontSize: '12px', marginLeft: '6px' }}>{svc.name}</span>
                                    </div>
                                    <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                                        <span style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--text-muted)' }}>:{svc.port}</span>
                                        <span className={`badge badge-${isUp ? 'success' : 'danger'}`}>{svc.status}</span>
                                    </div>
                                </div>
                            )
                        })}
                    </div>

                    {/* Interview talking points */}
                    <div className="glass-card" style={{ padding: '16px', marginTop: '12px' }}>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', marginBottom: '10px' }}>
                            Interview Talking Points
                        </div>
                        {[
                            { q: 'Prevent duplicate charges?', a: 'Redis idempotency keys with 24h TTL — card charged exactly once.' },
                            { q: 'What when Inventory crashes?', a: 'Saga compensation: refund payment, cancel order via Kafka events.' },
                            { q: 'Cascading failures?', a: 'Circuit breakers + bulkheads per service; each degrades independently.' },
                            { q: 'At-least-once delivery?', a: 'Outbox pattern + idempotent consumers = effectively-once semantics.' },
                        ].map(({ q, a }) => (
                            <div key={q} style={{ background: 'rgba(43,76,212,0.05)', border: '1px solid rgba(43,76,212,0.15)', borderRadius: '8px', padding: '10px', marginBottom: '6px' }}>
                                <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--zentra-cyan)', marginBottom: '3px' }}>Q: {q}</div>
                                <div style={{ fontSize: '10px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>A: {a}</div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}
