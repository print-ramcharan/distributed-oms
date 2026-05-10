import { useState, useEffect, useCallback } from 'react'
import { RefreshCw, Edit2, Check, X, Eye, ChevronDown, ChevronRight, Activity } from 'lucide-react'
import { SERVICE_DEFS, fetchAllServiceHealth, fetchLoggers, setLogLevel, fetchEnv } from '../api.js'

const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF']

function LogLevelBadge({ level }) {
    const getLevelStyle = (l) => {
        switch(l) {
            case 'TRACE': return 'bg-gray-50 text-gray-500 border-gray-200'
            case 'DEBUG': return 'bg-blue-50 text-blue-600 border-blue-200'
            case 'INFO': return 'bg-emerald-50 text-emerald-600 border-emerald-200'
            case 'WARN': return 'bg-orange-50 text-orange-600 border-orange-200'
            case 'ERROR': return 'bg-red-50 text-red-600 border-red-200'
            case 'OFF': return 'bg-gray-100 text-gray-400 border-gray-200'
            default: return 'bg-gray-50 text-gray-500 border-gray-200'
        }
    }
    
    return (
        <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold font-mono tracking-wider border ${getLevelStyle(level)}`}>
            {level || 'INHERITED'}
        </span>
    )
}

function ServicePanel({ svcDef }) {
    const [expanded, setExpanded] = useState(false)
    const [loggers, setLoggers] = useState(null)
    const [env, setEnv] = useState(null)
    const [editLevel, setEditLevel] = useState({})
    const [loading, setLoading] = useState(false)
    const [tab, setTab] = useState('loggers')

    const loadData = async () => {
        setLoading(true)
        const [l, e] = await Promise.all([fetchLoggers(svcDef), fetchEnv(svcDef)])
        setLoggers(l)
        setEnv(e)
        setLoading(false)
    }

    const toggleExpand = () => {
        setExpanded(v => !v)
        if (!expanded) loadData()
    }

    const applyLevel = async (loggerName, level) => {
        await setLogLevel(svcDef, loggerName, level)
        setEditLevel({})
        await loadData()
    }

    const loggerEntries = loggers?.loggers
        ? Object.entries(loggers.loggers).filter(([, v]) => v.configuredLevel).slice(0, 20)
        : []

    const envProps = env?.propertySources
        ? env.propertySources.find(s => s.name?.includes('application'))?.properties
        : null

    return (
        <div className="bg-white border border-gray-200 rounded-xl mb-4 shadow-sm overflow-hidden transition-all">
            <div
                onClick={toggleExpand}
                className={`p-4 flex justify-between items-center cursor-pointer hover:bg-gray-50 transition-colors ${expanded ? 'bg-gray-50/50' : ''}`}
            >
                <div className="flex items-center gap-3">
                    {expanded ? <ChevronDown size={16} className="text-gray-400" /> : <ChevronRight size={16} className="text-gray-400" />}
                    <span className="font-semibold text-sm text-gray-900">{svcDef.name}</span>
                    <span className="font-mono text-[10px] text-gray-400">:{svcDef.port}</span>
                </div>
                {loading && <RefreshCw size={14} className="text-gray-400 animate-spin" />}
            </div>

            {expanded && (
                <div className="border-t border-gray-200">
                    {/* Tabs */}
                    <div className="flex border-b border-gray-200 bg-gray-50/30">
                        {['loggers', 'environment'].map(t => (
                            <button
                                key={t}
                                onClick={() => setTab(t)}
                                className={`px-5 py-2.5 text-xs font-mono font-medium tracking-wide transition-colors ${
                                    tab === t 
                                        ? 'text-blue-600 border-b-2 border-blue-500 bg-blue-50/30' 
                                        : 'text-gray-500 border-b-2 border-transparent hover:text-gray-700 hover:bg-gray-50'
                                }`}
                            >
                                {t}
                            </button>
                        ))}
                    </div>

                    {tab === 'loggers' && (
                        <div className="p-4 max-h-[400px] overflow-y-auto">
                            {loggerEntries.length === 0 ? (
                                <div className="text-gray-500 text-xs py-4 text-center">
                                    {loggers === null ? 'Failed to connect — is the service running?' : 'No configured loggers found.'}
                                </div>
                            ) : (
                                <div className="space-y-1">
                                    {loggerEntries.map(([name, info]) => (
                                        <div key={name} className="flex justify-between items-center py-2 px-3 hover:bg-gray-50 rounded-lg group transition-colors">
                                            <span className="font-mono text-xs text-gray-600 flex-1 overflow-hidden text-ellipsis whitespace-nowrap pr-4">
                                                {name}
                                            </span>
                                            {editLevel[name] ? (
                                                <div className="flex items-center gap-2">
                                                    <select
                                                        value={editLevel[name]}
                                                        onChange={e => setEditLevel(prev => ({ ...prev, [name]: e.target.value }))}
                                                        className="text-xs bg-white border border-gray-300 text-gray-900 rounded px-2 py-1 font-mono focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                                    >
                                                        {LOG_LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
                                                    </select>
                                                    <button onClick={() => applyLevel(name, editLevel[name])} className="p-1 text-emerald-600 hover:bg-emerald-50 rounded"><Check size={14} /></button>
                                                    <button onClick={() => setEditLevel({})} className="p-1 text-red-500 hover:bg-red-50 rounded"><X size={14} /></button>
                                                </div>
                                            ) : (
                                                <div className="flex items-center gap-3">
                                                    <LogLevelBadge level={info.configuredLevel} />
                                                    <button
                                                        onClick={() => setEditLevel({ [name]: info.configuredLevel || 'INFO' })}
                                                        className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded opacity-0 group-hover:opacity-100 transition-all"
                                                    >
                                                        <Edit2 size={12} />
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {tab === 'environment' && (
                        <div className="p-4 max-h-[400px] overflow-y-auto">
                            {!envProps ? (
                                <div className="text-gray-500 text-xs py-4 text-center">
                                    {env === null ? 'Failed to connect — is the service running?' : 'No environment properties found.'}
                                </div>
                            ) : (
                                <div className="space-y-1.5">
                                    {Object.entries(envProps).slice(0, 25).map(([key, val]) => {
                                        const isSecret = val.value?.toString().toLowerCase().includes('pass') || val.value?.toString().toLowerCase().includes('secret');
                                        return (
                                            <div key={key} className="flex justify-between py-2 px-3 bg-gray-50 rounded-lg items-start gap-4">
                                                <span className="font-mono text-[11px] text-blue-600 min-w-[200px] break-all">{key}</span>
                                                <span className={`font-mono text-[11px] text-right flex-1 break-all ${isSecret ? 'text-gray-400 tracking-widest' : 'text-gray-700'}`}>
                                                    {isSecret ? '••••••••' : String(val.value)}
                                                </span>
                                            </div>
                                        )
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default function ServicesControl() {
    const [health, setHealth] = useState([])

    useEffect(() => {
        fetchAllServiceHealth().then(setHealth)
        const i = setInterval(() => fetchAllServiceHealth().then(setHealth), 5000)
        return () => clearInterval(i)
    }, [])

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
            <div>
                <h1 className="text-2xl font-semibold text-gray-900 tracking-tight flex items-center gap-2">
                    <Activity className="text-blue-600" size={24} /> Services Control
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                    Manage log levels and inspect Spring environment properties in real-time via Actuator.
                    Changes take effect immediately without restart.
                </p>
            </div>

            {/* Health summary bar */}
            <div className="flex flex-wrap gap-3 mb-6">
                {health.map(svc => {
                    const isUp = svc.status === 'UP'
                    return (
                        <div key={svc.id} className="flex items-center gap-2.5 px-3 py-2 bg-white border border-gray-200 rounded-lg shadow-sm">
                            <span className={`w-2 h-2 rounded-full ${isUp ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.5)]'}`} />
                            <span className="text-xs font-semibold text-gray-700">{svc.name}</span>
                            <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold font-mono tracking-wider uppercase ring-1 ring-inset ${
                                isUp ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 'bg-red-50 text-red-700 ring-red-600/20'
                            }`}>
                                {svc.status}
                            </span>
                        </div>
                    )
                })}
            </div>

            <div className="text-xs text-gray-400 font-mono tracking-wide uppercase mb-4">
                Click a service to expand and manage loggers / view environment config ↓
            </div>

            <div className="space-y-4">
                {SERVICE_DEFS.map(svc => (
                    <ServicePanel key={svc.id} svcDef={svc} />
                ))}
            </div>
        </div>
    )
}
