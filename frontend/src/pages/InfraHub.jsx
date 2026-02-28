import { useState } from 'react'
import { BarChart2, Layers, Radio, ExternalLink } from 'lucide-react'

const TOOLS = [
    {
        id: 'grafana',
        label: 'Grafana',
        desc: 'Dashboards for CPU, memory, HTTP throughput, error rates sourced from Prometheus',
        url: 'http://localhost:3000',
        embedUrl: 'http://localhost:3000/d/spring-boot/spring-boot-statistics?orgId=1&refresh=5s&kiosk',
        icon: BarChart2,
        color: 'var(--warning)',
        port: 3000,
        credentials: 'admin / admin',
    },
    {
        id: 'kafka-ui',
        label: 'Kafka UI',
        desc: 'Browse topics, consumer groups, messages, consumer lag, and partition details',
        url: 'http://localhost:8090',
        embedUrl: 'http://localhost:8090',
        icon: Layers,
        color: 'var(--zentra-cyan)',
        port: 8090,
    },
    {
        id: 'zipkin',
        label: 'Zipkin Traces',
        desc: 'End-to-end distributed tracing across all services for every order',
        url: 'http://localhost:9411',
        embedUrl: 'http://localhost:9411/zipkin/',
        icon: Radio,
        color: 'var(--zentra-purple)',
        port: 9411,
    },
]

export default function InfraHub() {
    const [active, setActive] = useState('grafana')

    const tool = TOOLS.find(t => t.id === active)

    return (
        <div style={{ padding: '28px 28px 0 28px', height: 'calc(100vh - 56px)', display: 'flex', flexDirection: 'column' }}>
            <div style={{ marginBottom: '16px' }}>
                <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>Infrastructure Hub</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                    Live embedded views of Grafana dashboards, Kafka UI topic browser, and Zipkin traces.
                    Start the infra stack first: <code style={{ fontFamily: "'Fira Code'", color: 'var(--zentra-cyan)' }}>docker compose up -d</code>
                </p>
            </div>

            {/* Tool tabs */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                {TOOLS.map(t => {
                    const Icon = t.icon
                    const isActive = t.id === active
                    return (
                        <button
                            key={t.id}
                            onClick={() => setActive(t.id)}
                            style={{
                                display: 'flex', alignItems: 'center', gap: '6px',
                                padding: '8px 16px', borderRadius: '10px', cursor: 'pointer',
                                border: `1px solid ${isActive ? t.color : 'var(--border)'}`,
                                background: isActive ? `${t.color}15` : 'transparent',
                                color: isActive ? t.color : 'var(--text-secondary)',
                                fontSize: '12px', fontWeight: isActive ? 600 : 400, transition: 'all 0.15s',
                            }}
                        >
                            <Icon size={14} /> {t.label}
                            <span style={{ fontSize: '10px', color: 'var(--text-muted)', fontFamily: "'Fira Code'" }}>:{t.port}</span>
                        </button>
                    )
                })}
                <a
                    href={tool.url}
                    target="_blank"
                    rel="noopener"
                    style={{
                        marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 12px',
                        borderRadius: '10px', background: 'transparent', border: '1px solid var(--border)',
                        color: 'var(--text-muted)', fontSize: '11px', textDecoration: 'none'
                    }}
                >
                    <ExternalLink size={12} /> Open in new tab
                </a>
            </div>

            {/* Tool info bar */}
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '8px 14px', borderRadius: '10px', background: 'rgba(255,255,255,0.02)',
                border: '1px solid var(--border)', marginBottom: '12px', fontSize: '12px'
            }}>
                <span style={{ color: 'var(--text-secondary)' }}>{tool.desc}</span>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    {tool.credentials && (
                        <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: 'var(--text-muted)' }}>
                            Login: {tool.credentials}
                        </span>
                    )}
                    <span style={{ fontFamily: "'Fira Code'", fontSize: '11px', color: tool.color }}>
                        localhost:{tool.port}
                    </span>
                </div>
            </div>

            {/* Iframe */}
            <div style={{ flex: 1, borderRadius: '16px', overflow: 'hidden', border: '1px solid var(--border)', background: '#000', marginBottom: '28px', position: 'relative' }}>
                <iframe
                    key={tool.id}
                    src={tool.embedUrl}
                    style={{ width: '100%', height: '100%', border: 'none' }}
                    title={tool.label}
                    sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
                    onError={() => { }}
                />
                {/* Overlay when not loaded */}
                <div style={{
                    position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: -1, pointerEvents: 'none',
                }}>
                    <div style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                        <div style={{ fontSize: '36px', marginBottom: '12px', opacity: 0.3 }}>📡</div>
                        <div style={{ fontSize: '13px', marginBottom: '6px' }}>Connecting to {tool.label}...</div>
                        <div style={{ fontFamily: "'Fira Code'", fontSize: '11px' }}>Make sure docker compose is running</div>
                    </div>
                </div>
            </div>
        </div>
    )
}
