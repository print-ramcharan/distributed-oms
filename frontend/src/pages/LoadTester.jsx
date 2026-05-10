import { Activity, ServerCrash, Clock, CheckCircle, RefreshCw, Zap, Database, Play, Square, TrendingUp } from 'lucide-react';
import { useLoadTest } from '../context/LoadTestContext';

const SCENARIO_TYPES = [
    { id: 'happy', label: 'Happy Path', color: 'emerald', desc: 'Valid product, qty=1' },
    { id: 'out_of_stock', label: 'Out of Stock', color: 'orange', desc: 'Valid product, qty=9999' },
    { id: 'invalid_product', label: 'Invalid Product', color: 'red', desc: 'Non-existent productId' },
    { id: 'read_orders', label: 'Read Orders', color: 'blue', desc: 'GET /orders (read load)' },
]

function getCapacityLabel(successRate, rps) {
    if (rps === 0) return { label: 'Idle', color: 'gray' }
    if (successRate >= 99) return { label: 'Healthy', color: 'emerald' }
    if (successRate >= 95) return { label: 'Degraded', color: 'yellow' }
    if (successRate >= 80) return { label: 'Stressed', color: 'orange' }
    return { label: 'Overloaded', color: 'red' }
}

export default function LoadTester() {
    const { 
        isRunning, products, isLoadingProducts, config, setConfig, 
        metrics, startTest, stopTest, loadProducts 
    } = useLoadTest()

    const capacity = getCapacityLabel(metrics.successRate, metrics.currentRps)
    const totalScenarioWeight = Object.values(config.scenarios).reduce((a, b) => a + b, 0)
    const maxRpsBar = Math.max(...metrics.rpsHistory, config.rps, 1)

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
            <div className="flex justify-between items-start">
                <div>
                    <h1 className="text-2xl font-semibold text-gray-900 tracking-tight">Load Tester</h1>
                    <p className="text-sm text-gray-500 mt-1">Real load against real services — uses live inventory products for all order scenarios.</p>
                </div>
                <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold ${
                    capacity.color === 'emerald' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' :
                    capacity.color === 'yellow' ? 'bg-yellow-50 text-yellow-700 border border-yellow-200' :
                    capacity.color === 'orange' ? 'bg-orange-50 text-orange-700 border border-orange-200' :
                    capacity.color === 'red' ? 'bg-red-50 text-red-700 border border-red-200' :
                    'bg-gray-50 text-gray-500 border border-gray-200'
                }`}>
                    <span className={`w-2 h-2 rounded-full ${isRunning ? 'animate-pulse' : ''} ${
                        capacity.color === 'emerald' ? 'bg-emerald-500' :
                        capacity.color === 'yellow' ? 'bg-yellow-500' :
                        capacity.color === 'orange' ? 'bg-orange-500' :
                        capacity.color === 'red' ? 'bg-red-500' : 'bg-gray-400'
                    }`} />
                    System: {capacity.label}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

                {/* LEFT: Config */}
                <div className="lg:col-span-4 space-y-4">

                    {/* Inventory Status */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <div className="flex items-center justify-between mb-3">
                            <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5"><Database size={12} /> Inventory (Live)</h3>
                            <button onClick={loadProducts} className="p-1 text-gray-400 hover:text-blue-600 rounded"><RefreshCw size={14} className={isLoadingProducts ? 'animate-spin' : ''} /></button>
                        </div>
                        {products.length === 0 ? (
                            <div className="text-xs text-gray-400 text-center py-3 border-dashed border border-gray-200 rounded-lg">
                                No products in inventory. <br/>Seed products in Order Simulator first.
                            </div>
                        ) : (
                            <div className="space-y-1 max-h-36 overflow-y-auto">
                                {products.map(p => (
                                    <div key={p.productId} className="flex justify-between items-center text-xs py-1.5 border-b border-gray-50 last:border-0">
                                        <span className="font-mono text-gray-700 truncate max-w-[60%]">{p.productId}</span>
                                        <span className={`font-bold px-1.5 py-0.5 rounded text-[10px] ${p.availableQuantity > 0 ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                                            {p.availableQuantity} avail
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* RPS */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <div className="flex justify-between mb-2">
                            <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5"><Zap size={12} /> Target RPS</label>
                            <span className="font-mono text-sm font-bold text-gray-900">{config.rps} / sec</span>
                        </div>
                        <input type="range" min="1" max="50" value={config.rps}
                            onChange={e => setConfig({ ...config, rps: parseInt(e.target.value) })}
                            className="w-full accent-blue-600" />
                        <p className="text-[10px] text-gray-400 mt-1">Browser limit ~50 RPS. For higher load, use k6/JMeter.</p>
                    </div>

                    {/* Scenario Mix */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3">Scenario Mix</h3>
                        <div className="space-y-3">
                            {SCENARIO_TYPES.map(s => (
                                <div key={s.id}>
                                    <div className="flex justify-between text-xs mb-1">
                                        <span className={`font-semibold text-${s.color}-700`}>{s.label}</span>
                                        <span className="font-mono text-gray-500">
                                            {totalScenarioWeight > 0 ? Math.round((config.scenarios[s.id] / totalScenarioWeight) * 100) : 0}%
                                        </span>
                                    </div>
                                    <input type="range" min="0" max="100" value={config.scenarios[s.id]}
                                        onChange={e => setConfig({ ...config, scenarios: { ...config.scenarios, [s.id]: parseInt(e.target.value) } })}
                                        className={`w-full accent-${s.color}-600`} />
                                    <p className="text-[10px] text-gray-400">{s.desc}</p>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Start/Stop */}
                    <button onClick={isRunning ? stopTest : startTest}
                        disabled={products.length === 0 && !isRunning}
                        className={`w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-xl text-sm font-semibold text-white transition-all disabled:opacity-40 ${
                            isRunning ? 'bg-red-500 hover:bg-red-600' : 'bg-gray-900 hover:bg-black'
                        }`}>
                        {isRunning ? <><Square size={16} fill="white" /> Stop Load Test</> : <><Play size={16} fill="white" /> Start Load Test</>}
                    </button>
                </div>

                {/* RIGHT: Live Metrics */}
                <div className="lg:col-span-8 space-y-4">

                    {/* Top KPI Row */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {[
                            { icon: <Activity size={16} className="text-blue-500" />, label: 'Current RPS', value: metrics.currentRps, unit: '/s' },
                            { icon: <CheckCircle size={16} className="text-emerald-500" />, label: 'Success', value: metrics.successCount },
                            { icon: <ServerCrash size={16} className="text-red-500" />, label: 'Errors', value: metrics.errorCount },
                            { icon: <TrendingUp size={16} className="text-purple-500" />, label: 'Success Rate', value: `${metrics.successRate}%` },
                        ].map(({ icon, label, value, unit }) => (
                            <div key={label} className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                                <div className="flex items-center gap-1.5 text-xs text-gray-500 font-medium mb-1">{icon}{label}</div>
                                <div className="text-2xl font-mono font-bold text-gray-900">{value}<span className="text-xs text-gray-400 ml-1">{unit}</span></div>
                            </div>
                        ))}
                    </div>

                    {/* Throughput Sparkline */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <div className="flex justify-between items-center mb-3">
                            <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Throughput (last 30s)</h3>
                            <span className="font-mono text-xs text-blue-600">{metrics.currentRps} rps live</span>
                        </div>
                        <div className="flex items-end gap-0.5 h-16">
                            {Array.from({ length: 30 }, (_, i) => {
                                const val = metrics.rpsHistory[i] ?? 0
                                const heightPct = maxRpsBar > 0 ? (val / maxRpsBar) * 100 : 0
                                return (
                                    <div key={i} className="flex-1 flex flex-col justify-end">
                                        <div
                                            className={`rounded-sm transition-all duration-300 ${isRunning ? 'bg-blue-400' : 'bg-gray-200'}`}
                                            style={{ height: `${Math.max(2, heightPct)}%` }}
                                        />
                                    </div>
                                )
                            })}
                        </div>
                    </div>

                    {/* Latency */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3 flex items-center gap-1.5"><Clock size={12} /> Latency Percentiles (ms)</h3>
                        <div className="grid grid-cols-3 gap-4">
                            {[['P50 (Median)', metrics.p50, 'emerald', 200], ['P95', metrics.p95, 'yellow', 500], ['P99', metrics.p99, 'red', 1000]].map(([label, val, color, threshold]) => (
                                <div key={label} className={`text-center p-3 rounded-lg border ${val > threshold ? `bg-${color}-50 border-${color}-100` : 'bg-gray-50 border-gray-100'}`}>
                                    <div className="text-[10px] text-gray-500 font-medium mb-1">{label}</div>
                                    <div className={`text-2xl font-mono font-bold ${val > threshold ? `text-${color}-700` : 'text-gray-900'}`}>
                                        {val}<span className="text-xs text-gray-400 ml-1">ms</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Per-Scenario Breakdown */}
                    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
                        <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3">Scenario Breakdown</h3>
                        <div className="space-y-2">
                            {SCENARIO_TYPES.map(s => {
                                const sc = metrics.byScenario[s.id]
                                const scTotal = sc.success + sc.error
                                const scRate = scTotal === 0 ? 0 : Math.round((sc.success / scTotal) * 100)
                                return (
                                    <div key={s.id} className="flex items-center gap-3">
                                        <div className="w-32 text-xs font-semibold text-gray-700 truncate">{s.label}</div>
                                        <div className="flex-1 bg-gray-100 rounded-full h-2 overflow-hidden">
                                            <div className={`h-full rounded-full bg-${s.color}-500 transition-all duration-500`} style={{ width: `${scRate}%` }} />
                                        </div>
                                        <div className="w-12 text-right text-xs font-mono text-gray-500">{scTotal === 0 ? '—' : `${scRate}%`}</div>
                                        <div className="w-20 text-right text-[10px] text-gray-400 font-mono">{sc.success}✓ {sc.error}✗</div>
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
