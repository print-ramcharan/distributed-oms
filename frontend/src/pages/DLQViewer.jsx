import { useState, useEffect, useCallback } from 'react'
import { Inbox, RefreshCw, Eye, Trash2, ChevronRight, AlertTriangle, Clock } from 'lucide-react'
import { fetchDlqMessages, retryDlqMessage, resolveDlqMessage } from '../api.js'

function DLQRow({ msg, onReplay, onDismiss }) {
    const [expanded, setExpanded] = useState(false)
    const [loading, setLoading] = useState(null)

    const ago = msg.failedAt
        ? Math.max(0, Math.round((Date.now() - new Date(msg.failedAt).getTime()) / 60000))
        : '?'

    let payloadDisplay = null
    try {
        const parsed = typeof msg.payload === 'string' ? JSON.parse(msg.payload) : msg.payload
        payloadDisplay = JSON.stringify(parsed, null, 2)
    } catch {
        payloadDisplay = String(msg.payload || '{}')
    }

    const handleReplay = async () => {
        setLoading('replay')
        await onReplay(msg.id)
        setLoading(null)
    }

    const handleDismiss = async () => {
        setLoading('dismiss')
        await onDismiss(msg.id)
        setLoading(null)
    }

    return (
        <div className="glass-card" style={{ padding: '16px', marginBottom: '10px', borderLeft: '3px solid var(--danger)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                        <AlertTriangle size={13} color="var(--danger)" />
                        <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)' }}>
                            {String(msg.id).substring(0, 8).toUpperCase()}
                        </span>
                        <span className="badge badge-danger">DLQ</span>
                        {msg.originalTopic && <span className="badge badge-pending">{msg.originalTopic.toUpperCase()}</span>}
                        {msg.status && <span className={`badge badge-${msg.status === 'PENDING' ? 'warning' : msg.status === 'RETRIED' ? 'info' : 'success'}`}>{msg.status}</span>}
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--danger)', marginBottom: '4px' }}>
                        {msg.errorMessage || msg.errorMsg || 'Unknown error'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", display: 'flex', gap: '12px' }}>
                        <span>{msg.retryCount ?? msg.retries ?? 0} retries</span>
                        <span>·</span>
                        <span>{ago}m ago</span>
                        {msg.aggregateId && <><span>·</span><span>orderId: {String(msg.aggregateId).substring(0, 8)}</span></>}
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '6px', marginLeft: '12px' }}>
                    <button className="btn btn-ghost" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => setExpanded(e => !e)}>
                        <Eye size={12} /> {expanded ? 'Close' : 'Inspect'}
                    </button>
                    <button className="btn btn-primary" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={handleReplay} disabled={loading === 'replay' || msg.status === 'RETRIED'}>
                        <RefreshCw size={12} style={{ animation: loading === 'replay' ? 'spin 1s linear infinite' : 'none' }} />
                        {loading === 'replay' ? 'Retrying...' : 'Replay'}
                    </button>
                    <button className="btn btn-ghost" style={{ padding: '6px 10px', fontSize: '11px', color: 'var(--danger)', borderColor: 'rgba(239,68,68,0.3)' }} onClick={handleDismiss} disabled={loading === 'dismiss'}>
                        <Trash2 size={12} />
                    </button>
                </div>
            </div>

            {expanded && (
                <div style={{ marginTop: '12px', borderTop: '1px solid var(--border)', paddingTop: '12px' }}>
                    <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", marginBottom: '6px', textTransform: 'uppercase' }}>
                        Raw Payload · Topic: {msg.originalTopic}
                    </div>
                    <pre style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)', background: 'rgba(0,0,0,0.3)', borderRadius: '8px', padding: '10px', overflowX: 'auto', border: '1px solid var(--border)', margin: 0 }}>
                        {payloadDisplay}
                    </pre>
                    {msg.stackTrace && (
                        <>
                            <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", margin: '10px 0 4px', textTransform: 'uppercase' }}>Stack Trace</div>
                            <pre style={{ fontFamily: "'Fira Code'", fontSize: '10px', color: 'var(--danger)', background: 'rgba(239,68,68,0.05)', borderRadius: '8px', padding: '10px', overflowX: 'auto', border: '1px solid rgba(239,68,68,0.2)', margin: 0, maxHeight: '150px' }}>
                                {msg.stackTrace}
                            </pre>
                        </>
                    )}
                </div>
            )}
        </div>
    )
}

export default function DLQViewer() {
    const [messages, setMessages] = useState([])
    const [loading, setLoading] = useState(true)
    const [filter, setFilter] = useState('ALL')
    const [replayedCount, setReplayedCount] = useState(0)

    const loadMessages = useCallback(async () => {
        setLoading(true)
        const data = await fetchDlqMessages(filter === 'ALL' ? null : filter)
        setMessages(data || [])
        setLoading(false)
    }, [filter])

    useEffect(() => {
        loadMessages()
        const interval = setInterval(loadMessages, 8000)
        return () => clearInterval(interval)
    }, [loadMessages])

    const handleReplay = async (id) => {
        const result = await retryDlqMessage(id)
        if (result) {
            setReplayedCount(c => c + 1)
            await loadMessages()
        }
    }

    const handleDismiss = async (id) => {
        await resolveDlqMessage(id)
        await loadMessages()
    }

    const pendingCount = messages.filter(m => m.status === 'PENDING').length

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>DLQ Viewer</h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                        Dead Letter Queue — messages that failed after{' '}
                        <span style={{ fontFamily: "'Fira Code'", color: 'var(--zentra-cyan)' }}>3 retries</span>.
                        Source: <span style={{ fontFamily: "'Fira Code'", color: 'var(--text-muted)' }}>GET /admin/dlq</span>
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <button className="btn btn-ghost" style={{ padding: '6px 12px', fontSize: '11px' }} onClick={loadMessages}>
                        <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} /> Refresh
                    </button>
                    <span className={`badge badge-${pendingCount > 0 ? 'danger' : 'success'}`}>{pendingCount} Pending</span>
                    {replayedCount > 0 && <span className="badge badge-success">{replayedCount} Replayed</span>}
                </div>
            </div>

            {/* Stat Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', marginBottom: '20px' }}>
                {[
                    { label: 'Total Messages', value: messages.length, color: 'var(--danger)' },
                    { label: 'Pending Review', value: pendingCount, color: 'var(--warning)' },
                    { label: 'Replayed this Session', value: replayedCount, color: 'var(--success)' },
                ].map(s => (
                    <div key={s.label} className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
                        <div style={{ fontFamily: "'Fira Code'", fontSize: '28px', fontWeight: 700, color: s.color }}>{s.value}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>{s.label}</div>
                    </div>
                ))}
            </div>

            {/* Filter */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
                {['ALL', 'PENDING', 'RETRIED', 'RESOLVED'].map(f => (
                    <button
                        key={f}
                        onClick={() => setFilter(f)}
                        className={`btn btn-ghost`}
                        style={{ fontSize: '11px', padding: '5px 12px', border: `1px solid ${filter === f ? 'var(--zentra-cyan)' : 'var(--border)'}`, color: filter === f ? 'var(--zentra-cyan)' : 'var(--text-muted)' }}
                    >
                        {f}
                    </button>
                ))}
            </div>

            {/* Messages */}
            {loading && messages.length === 0 ? (
                <div className="glass-card" style={{ padding: '48px', textAlign: 'center' }}>
                    <RefreshCw size={32} color="var(--text-muted)" style={{ margin: '0 auto 12px', animation: 'spin 1s linear infinite' }} />
                    <div style={{ color: 'var(--text-muted)', fontSize: '12px' }}>Loading DLQ from backend...</div>
                </div>
            ) : messages.length === 0 ? (
                <div className="glass-card" style={{ padding: '48px', textAlign: 'center' }}>
                    <Inbox size={40} color="var(--success)" style={{ margin: '0 auto 16px' }} />
                    <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--success)', marginBottom: '6px' }}>
                        {loading ? 'Loading...' : 'Queue is empty!'}
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                        {loading ? 'Fetching from /admin/dlq...' : 'No messages match this filter.'}
                    </div>
                </div>
            ) : (
                messages.map(msg => (
                    <DLQRow key={msg.id} msg={msg} onReplay={handleReplay} onDismiss={handleDismiss} />
                ))
            )}

            {/* DLQ Flow Diagram */}
            <div className="glass-card" style={{ padding: '20px', marginTop: '24px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>
                    How DLQ Works in Zentra
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    {['Event Published', 'Consumer Fails', 'Retry 1 (1s)', 'Retry 2 (2s)', 'Retry 3 (4s)', '→ Dead Letter Queue', 'Manual Review', 'Replay / Dismiss'].map((step, i, arr) => (
                        <span key={step} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{
                                fontFamily: "'Fira Code'", fontSize: '11px',
                                color: i >= 5 ? 'var(--zentra-cyan)' : 'var(--text-secondary)',
                                background: i >= 5 ? 'rgba(0,184,217,0.08)' : 'rgba(255,255,255,0.03)',
                                border: `1px solid ${i >= 5 ? 'rgba(0,184,217,0.2)' : 'var(--border)'}`,
                                padding: '4px 10px', borderRadius: '6px'
                            }}>
                                {step}
                            </span>
                            {i < arr.length - 1 && <ChevronRight size={12} color="var(--text-muted)" />}
                        </span>
                    ))}
                </div>
            </div>
        </div>
    )
}
