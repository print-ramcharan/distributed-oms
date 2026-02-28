import { useState } from 'react'
import { Inbox, RefreshCw, Eye, Trash2, ChevronRight, AlertTriangle } from 'lucide-react'
import { useSagaStore } from '../store.js'

function DLQRow({ msg, onReplay, onDismiss }) {
    const [expanded, setExpanded] = useState(false)
    const ago = Math.round((Date.now() - msg.timestamp) / 60000)

    return (
        <div className="glass-card" style={{ padding: '16px', marginBottom: '10px', borderLeft: '3px solid var(--danger)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                        <AlertTriangle size={13} color="var(--danger)" />
                        <span style={{ fontFamily: "'Fira Code'", fontSize: '12px', color: 'var(--zentra-cyan)' }}>{msg.id}</span>
                        <span className="badge badge-danger">DLQ</span>
                        <span className="badge badge-pending">{msg.topic}</span>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--danger)', marginBottom: '4px' }}>{msg.errorMsg}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: "'Fira Code'" }}>
                        {msg.retries} retries — {ago}m ago
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '6px', marginLeft: '12px' }}>
                    <button className="btn btn-ghost" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => setExpanded(e => !e)}>
                        <Eye size={12} /> Inspect
                    </button>
                    <button className="btn btn-primary" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => onReplay(msg.id)}>
                        <RefreshCw size={12} /> Replay
                    </button>
                    <button className="btn btn-ghost" style={{ padding: '6px 10px', fontSize: '11px', color: 'var(--danger)', borderColor: 'rgba(239,68,68,0.3)' }} onClick={() => onDismiss(msg.id)}>
                        <Trash2 size={12} />
                    </button>
                </div>
            </div>

            {expanded && (
                <div style={{ marginTop: '12px', borderTop: '1px solid var(--border)', paddingTop: '12px' }}>
                    <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", marginBottom: '6px', textTransform: 'uppercase' }}>Raw Payload</div>
                    <pre style={{
                        fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--zentra-cyan)',
                        background: 'rgba(0,0,0,0.3)', borderRadius: '8px', padding: '10px', overflowX: 'auto',
                        border: '1px solid var(--border)'
                    }}>
                        {JSON.stringify(JSON.parse(msg.payload), null, 2)}
                    </pre>
                </div>
            )}
        </div>
    )
}

export default function DLQViewer() {
    const { dlqMessages, replayDlqMessage } = useSagaStore()
    const [dismissed, setDismissed] = useState([])
    const [replayed, setReplayed] = useState([])

    const handleReplay = (id) => {
        replayDlqMessage(id)
        setReplayed(r => [...r, id])
    }

    const handleDismiss = (id) => {
        setDismissed(d => [...d, id])
    }

    const visible = dlqMessages.filter(m => !dismissed.includes(m.id))

    return (
        <div style={{ padding: '28px' }}>
            <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>DLQ Viewer</h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                        Dead Letter Queue — messages that failed after {' '}
                        <span style={{ fontFamily: "'Fira Code'", color: 'var(--zentra-cyan)' }}>3 retries</span>.
                        Inspect, replay, or dismiss.
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <span className={`badge badge-${visible.length > 0 ? 'danger' : 'success'}`}>
                        {visible.length} Pending
                    </span>
                    {replayed.length > 0 && <span className="badge badge-success">{replayed.length} Replayed</span>}
                </div>
            </div>

            {/* DLQ Explanation */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', marginBottom: '24px' }}>
                {[
                    { label: 'Total Messages', value: dlqMessages.length, color: 'var(--danger)' },
                    { label: 'Pending Review', value: visible.length, color: 'var(--warning)' },
                    { label: 'Replayed Today', value: replayed.length, color: 'var(--success)' },
                ].map(s => (
                    <div key={s.label} className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
                        <div style={{ fontFamily: "'Fira Code'", fontSize: '28px', fontWeight: 700, color: s.color }}>{s.value}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>{s.label}</div>
                    </div>
                ))}
            </div>

            {/* Messages */}
            {visible.length === 0 ? (
                <div className="glass-card" style={{ padding: '48px', textAlign: 'center' }}>
                    <Inbox size={40} color="var(--success)" style={{ margin: '0 auto 16px' }} />
                    <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--success)', marginBottom: '6px' }}>Queue is empty!</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>All messages have been successfully replayed or dismissed.</div>
                </div>
            ) : (
                visible.map(msg => (
                    <DLQRow key={msg.id} msg={msg} onReplay={handleReplay} onDismiss={handleDismiss} />
                ))
            )}

            {/* How it works */}
            <div className="glass-card" style={{ padding: '20px', marginTop: '24px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: "'Fira Code'", textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>How DLQ Works in Zentra</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    {['Event Published', 'Consumer Fails', 'Retry 1 (1s)', 'Retry 2 (2s)', 'Retry 3 (4s)', '→ Dead Letter Queue', 'Manual Review', 'Replay / Dismiss'].map((step, i, arr) => (
                        <>
                            <span key={step} style={{
                                fontFamily: "'Fira Code'", fontSize: '11px',
                                color: i >= 5 ? 'var(--zentra-cyan)' : 'var(--text-secondary)',
                                background: i >= 5 ? 'rgba(0,184,217,0.08)' : 'rgba(255,255,255,0.03)',
                                border: `1px solid ${i >= 5 ? 'rgba(0,184,217,0.2)' : 'var(--border)'}`,
                                padding: '4px 10px', borderRadius: '6px'
                            }}>
                                {step}
                            </span>
                            {i < arr.length - 1 && <ChevronRight size={12} color="var(--text-muted)" key={`arr-${i}`} />}
                        </>
                    ))}
                </div>
            </div>
        </div>
    )
}
