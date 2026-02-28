import { useState, useEffect, useCallback } from 'react'
import { RefreshCw, Edit2, Check, X, Eye, ChevronDown, ChevronRight } from 'lucide-react'
import { SERVICE_DEFS, fetchAllServiceHealth, fetchLoggers, setLogLevel, fetchEnv } from '../api.js'

const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF']

function LogLevelBadge({ level }) {
    const color = { TRACE: 'var(--text-muted)', DEBUG: 'var(--zentra-cyan)', INFO: 'var(--success)', WARN: 'var(--warning)', ERROR: 'var(--danger)', OFF: 'var(--text-muted)' }[level] || 'var(--text-muted)'
    return <span className="badge" style={{ background: `${color}18`, color, border: `1px solid ${color}44`, fontFamily: "'Fira Code'", fontSize: '10px' }}>{level || 'INHERITED'}</span>
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
        <div className="glass-card" style={{ marginBottom: '10px', overflow: 'hidden' }}>
            <div
                onClick={toggleExpand}
                style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
            >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {expanded ? <ChevronDown size={14} color="var(--text-muted)" /> : <ChevronRight size={14} color="var(--text-muted)" />}
                    <span style={{ fontWeight: 600, fontSize: '13px' }}>{svcDef.name}</span>
                    <span style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--text-muted)' }}>:{svcDef.port}</span>
                </div>
                {loading && <RefreshCw size={13} color="var(--text-muted)" style={{ animation: 'spin 1s linear infinite' }} />}
            </div>

            {expanded && (
                <div style={{ borderTop: '1px solid var(--border)' }}>
                    {/* Tabs */}
                    <div style={{ display: 'flex', gap: '0', borderBottom: '1px solid var(--border)' }}>
                        {['loggers', 'environment'].map(t => (
                            <button
                                key={t}
                                onClick={() => setTab(t)}
                                style={{
                                    padding: '8px 18px', fontSize: '12px', background: tab === t ? 'rgba(0,184,217,0.07)' : 'transparent',
                                    border: 'none', borderBottom: `2px solid ${tab === t ? 'var(--zentra-cyan)' : 'transparent'}`,
                                    color: tab === t ? 'var(--zentra-cyan)' : 'var(--text-muted)', cursor: 'pointer', fontFamily: "'Fira Code'"
                                }}
                            >
                                {t}
                            </button>
                        ))}
                    </div>

                    {tab === 'loggers' && (
                        <div style={{ padding: '14px 20px', maxHeight: '280px', overflowY: 'auto' }}>
                            {loggerEntries.length === 0 ? (
                                <div style={{ color: 'var(--text-muted)', fontSize: '12px', padding: '12px 0' }}>
                                    {loggers === null ? 'Failed to connect — is the service running?' : 'No configured loggers found.'}
                                </div>
                            ) : (
                                loggerEntries.map(([name, info]) => (
                                    <div key={name} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '5px 0', borderBottom: '1px solid rgba(255,255,255,0.03)', gap: '8px' }}>
                                        <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--text-secondary)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                            {name}
                                        </span>
                                        {editLevel[name] ? (
                                            <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                                                <select
                                                    value={editLevel[name]}
                                                    onChange={e => setEditLevel(prev => ({ ...prev, [name]: e.target.value }))}
                                                    style={{ fontSize: '11px', background: 'var(--bg-secondary)', border: '1px solid var(--border)', color: 'var(--text-primary)', borderRadius: '4px', padding: '2px 4px', fontFamily: "'Fira Code'" }}
                                                >
                                                    {LOG_LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
                                                </select>
                                                <button onClick={() => applyLevel(name, editLevel[name])} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--success)' }}><Check size={12} /></button>
                                                <button onClick={() => setEditLevel({})} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--danger)' }}><X size={12} /></button>
                                            </div>
                                        ) : (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                                <LogLevelBadge level={info.configuredLevel} />
                                                <button
                                                    onClick={() => setEditLevel({ [name]: info.configuredLevel || 'INFO' })}
                                                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '2px' }}
                                                >
                                                    <Edit2 size={11} />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>
                    )}

                    {tab === 'environment' && (
                        <div style={{ padding: '14px 20px', maxHeight: '280px', overflowY: 'auto' }}>
                            {!envProps ? (
                                <div style={{ color: 'var(--text-muted)', fontSize: '12px', padding: '12px 0' }}>
                                    {env === null ? 'Failed to connect — is the service running?' : 'No environment properties found.'}
                                </div>
                            ) : (
                                Object.entries(envProps).slice(0, 25).map(([key, val]) => (
                                    <div key={key} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px solid rgba(255,255,255,0.03)', gap: '12px', alignItems: 'flex-start' }}>
                                        <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)', minWidth: '200px' }}>{key}</span>
                                        <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: val.value?.toString().includes('pass') || val.value?.toString().includes('secret') ? 'var(--text-muted)' : 'var(--text-secondary)', textAlign: 'right', flex: 1 }}>
                                            {val.value?.toString().includes('pass') || val.value?.toString().includes('secret') ? '••••••••' : String(val.value)}
                                        </span>
                                    </div>
                                ))
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
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>Services Control</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                    Manage log levels and inspect Spring environment properties in real-time via Actuator.
                    Changes take effect immediately without restart.
                </p>
            </div>

            {/* Health summary bar */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
                {health.map(svc => (
                    <div key={svc.id} style={{
                        display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '8px',
                        background: 'rgba(255,255,255,0.02)', border: '1px solid var(--border)', fontSize: '11px'
                    }}>
                        <span className={`pulse-dot pulse-${svc.status === 'UP' ? 'success' : 'danger'}`} />
                        <span style={{ marginLeft: '6px', color: 'var(--text-secondary)' }}>{svc.name}</span>
                        <span className={`badge badge-${svc.status === 'UP' ? 'success' : 'danger'}`} style={{ fontSize: '9px' }}>{svc.status}</span>
                    </div>
                ))}
            </div>

            <div style={{ marginBottom: '14px', fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'" }}>
                Click a service to expand and manage loggers / view environment config ↓
            </div>

            {SERVICE_DEFS.map(svc => (
                <ServicePanel key={svc.id} svcDef={svc} />
            ))}
        </div>
    )
}
