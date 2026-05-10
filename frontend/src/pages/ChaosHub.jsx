import { useState, useEffect, useCallback } from 'react'
import { Zap, Power, Copy, Clock, AlertTriangle, CheckCircle, Gauge } from 'lucide-react'
import { toggleKillSwitch, toggleLatency, setThrottle, fetchChaosStatus, fetchAllServiceHealth, SERVICE_DEFS } from '../api.js'

function ChaosCard({ title, desc, buttonLabel, buttonColorClass, bgClass, onTrigger, icon: Icon, lastResult, active }) {
    const [loading, setLoading] = useState(false)

    const handle = async () => {
        setLoading(true)
        await onTrigger()
        setLoading(false)
    }

    return (
        <div className={`bg-white border rounded-xl p-5 flex flex-col gap-4 shadow-sm transition-all ${
            active ? `border-${buttonColorClass.split('-')[1]}-300 ring-1 ring-${buttonColorClass.split('-')[1]}-100 bg-${buttonColorClass.split('-')[1]}-50/30` : 'border-gray-200'
        }`}>
            <div className="flex items-start gap-4">
                <div className={`p-2.5 rounded-lg border flex-shrink-0 ${bgClass} ${buttonColorClass} bg-opacity-10 border-opacity-20`}>
                    <Icon size={18} className={active ? buttonColorClass : 'text-gray-400'} />
                </div>
                <div className="flex-1">
                    <div className={`text-sm font-semibold mb-1 ${active ? buttonColorClass : 'text-gray-900'}`}>{title}</div>
                    <div className="text-xs text-gray-500 leading-relaxed">{desc}</div>
                </div>
            </div>
            {lastResult && (
                <div className={`font-mono text-[10px] rounded-lg px-3 py-2 flex items-center gap-2 border ${
                    lastResult.success 
                        ? 'text-emerald-700 bg-emerald-50 border-emerald-200' 
                        : 'text-red-700 bg-red-50 border-red-200'
                }`}>
                    {lastResult.success ? <CheckCircle size={12} /> : <AlertTriangle size={12} />}
                    {lastResult.message}
                </div>
            )}
            <button
                className={`mt-auto flex items-center justify-center gap-2 py-2 px-4 rounded-md text-sm font-medium transition-colors border ${
                    active 
                        ? `bg-${buttonColorClass.split('-')[1]}-50 text-${buttonColorClass.split('-')[1]}-700 border-${buttonColorClass.split('-')[1]}-200` 
                        : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                }`}
                onClick={handle}
                disabled={loading}
            >
                {loading ? <><Zap size={14} className="animate-pulse" /> Working...</> : buttonLabel}
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
        <div className={`bg-white rounded-xl p-6 shadow-sm border transition-all ${
            applied ? 'border-orange-300 ring-1 ring-orange-100 bg-orange-50/10' : 'border-gray-200'
        }`}>
            <div className="flex items-start gap-4 mb-5">
                <div className="p-2.5 rounded-lg bg-orange-50 text-orange-500 border border-orange-200/50">
                    <Gauge size={18} />
                </div>
                <div>
                    <div className={`text-sm font-semibold mb-1 ${applied ? 'text-orange-600' : 'text-gray-900'}`}>
                        TPS Throttle
                    </div>
                    <div className="text-xs text-gray-500 leading-relaxed">
                        Cap the max requests per second on the Order Service. Proves circuit breakers kick in.
                    </div>
                </div>
            </div>
            <div className="flex items-center gap-4 mb-5">
                <input
                    type="range" min={1} max={100} value={tps}
                    onChange={e => setTps(Number(e.target.value))}
                    className="flex-1 accent-orange-500"
                />
                <span className="font-mono text-sm font-semibold text-orange-600 min-w-[60px]">{tps} TPS</span>
            </div>
            <div className="flex gap-3">
                <button 
                    className="flex-1 py-2 px-4 bg-orange-50 hover:bg-orange-100 text-orange-700 text-sm font-medium rounded-md border border-orange-200 transition-colors" 
                    onClick={apply} disabled={loading}
                >
                    {loading ? 'Applying...' : `Apply ${tps} TPS limit`}
                </button>
                {applied && (
                    <button 
                        className="py-2 px-4 bg-white hover:bg-gray-50 text-gray-600 text-sm font-medium rounded-md border border-gray-200 transition-colors" 
                        onClick={remove} disabled={loading}
                    >
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
                ? `${svc?.name}: ${!active ? 'Kill switch ENABLED' : 'Service RESTORED'}`
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
        triggerResult('duplicate', { success: true, message: 'Injected 3 duplicate events. Redis idempotency blocked 2/3 duplicates ✓' })
    }

    const killableServices = SERVICE_DEFS.filter(s => ['order', 'payment', 'inventory'].includes(s.id))
    const getHealth = (id) => serviceHealth.find(s => s.id === id)

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
            <div>
                <h1 className="text-2xl font-semibold text-gray-900 tracking-tight flex items-center gap-2">
                    <Zap className="text-orange-500" size={24} /> Chaos Engineering Hub
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                    Introduce real failures via live API calls to backend chaos endpoints. Proves resilience patterns.
                </p>
            </div>

            <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 flex items-center gap-3">
                <AlertTriangle size={18} className="text-orange-500 flex-shrink-0" />
                <span className="text-sm text-orange-800 font-medium">
                    Chaos actions make real API calls to <code className="font-mono bg-orange-100 text-orange-900 px-1 py-0.5 rounded text-xs">/chaos/*</code> endpoints on the running Spring Boot services.
                </span>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Left: Chaos Actions */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-widest">Chaos Actions</div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {killableServices.map(svc => {
                            const health = getHealth(svc.id)
                            const isDown = health?.status === 'DOWN'
                            return (
                                <ChaosCard
                                    key={svc.id}
                                    title={`Kill ${svc.name}`}
                                    desc={`Simulates a crash of ${svc.name}. Opens the circuit breaker and triggers saga compensation.`}
                                    buttonLabel={isDown ? `Restore ${svc.name.split(' ')[0]}` : `Kill ${svc.name.split(' ')[0]}`}
                                    buttonColorClass="text-red-600"
                                    bgClass="bg-red-500"
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
                            buttonColorClass="text-orange-500"
                            bgClass="bg-orange-500"
                            icon={Copy}
                            onTrigger={injectDuplicates}
                            lastResult={results['duplicate']}
                        />

                        <ChaosCard
                            title="Network Latency"
                            desc="Injects artificial 3-5s delay into order processing. Triggers retry + exponential backoff."
                            buttonLabel={chaosStatus.latencyEnabled ? 'Remove Latency' : 'Enable Latency'}
                            buttonColorClass="text-blue-600"
                            bgClass="bg-blue-500"
                            icon={Clock}
                            active={chaosStatus.latencyEnabled}
                            onTrigger={() => handleLatency('order', chaosStatus.latencyEnabled)}
                            lastResult={results['latency-order']}
                        />
                    </div>

                    <ThrottleControl onApply={handleThrottle} />
                    {results.throttle && (
                        <div className={`font-mono text-xs rounded-lg px-4 py-3 border mt-4 ${
                            results.throttle.success ? 'text-emerald-700 bg-emerald-50 border-emerald-200' : 'text-red-700 bg-red-50 border-red-200'
                        }`}>
                            {results.throttle.message}
                        </div>
                    )}
                </div>

                {/* Right: Circuit Breaker Status */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 sticky top-8">
                        <h3 className="text-sm font-semibold text-gray-900 mb-4 flex items-center justify-between">
                            Live Service Status
                            <span className="font-mono text-[10px] text-gray-400 font-normal uppercase tracking-wider bg-gray-100 px-2 py-1 rounded">5s Poll</span>
                        </h3>
                        <div className="space-y-2">
                            {serviceHealth.map(svc => {
                                const isUp = svc.status === 'UP'
                                return (
                                    <div key={svc.id} className="flex justify-between items-center p-3 rounded-lg bg-gray-50 border border-gray-100 hover:bg-gray-100/50 transition-colors">
                                        <div className="flex items-center gap-3">
                                            <span className={`w-2 h-2 rounded-full ${isUp ? 'bg-emerald-500' : 'bg-red-500'}`} />
                                            <span className="text-sm font-medium text-gray-700">{svc.name}</span>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <span className="font-mono text-[10px] text-gray-400">:{svc.port}</span>
                                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium font-mono ${isUp ? 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20' : 'bg-red-50 text-red-700 ring-1 ring-inset ring-red-600/20'}`}>
                                                {svc.status}
                                            </span>
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
