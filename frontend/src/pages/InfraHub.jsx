import { useState, useEffect } from 'react'
import { Activity, Server, Cpu, HardDrive, ExternalLink, BarChart2, Layers, Radio, RefreshCw, AlertTriangle, Hash, MessageSquare } from 'lucide-react'
import { fetchAllServiceHealth, fetchAllServiceMetrics } from '../api.js'

const TOOLS = [
    {
        id: 'grafana',
        label: 'Grafana',
        desc: 'Dashboards for CPU, memory, HTTP throughput, error rates sourced from Prometheus',
        url: 'http://localhost:3000',
        embedUrl: 'http://localhost:3000/d/oms-overview/distributed-oms-service-overview?orgId=1&refresh=5s&kiosk',
        icon: BarChart2,
        colorClass: 'orange',
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
        colorClass: 'blue',
        port: 8090,
    },
    {
        id: 'zipkin',
        label: 'Zipkin Traces',
        desc: 'End-to-end distributed tracing across all services for every order',
        url: 'http://localhost:9411',
        embedUrl: 'http://localhost:9411/zipkin/',
        icon: Radio,
        colorClass: 'purple',
        port: 9411,
    },
]

function ProgressBar({ percent, colorClass }) {
    return (
        <div className="w-full bg-gray-100 rounded-full h-1.5 mt-2 overflow-hidden">
            <div className={`h-1.5 rounded-full ${colorClass} transition-all duration-500`} style={{ width: `${Math.min(percent, 100)}%` }} />
        </div>
    )
}

function ServiceCard({ service }) {
    const isUp = service.status === 'UP'
    
    // CPU
    const cpuPct = service.cpuPercent || 0
    const cpuColor = cpuPct > 80 ? 'bg-red-500' : cpuPct > 50 ? 'bg-orange-500' : 'bg-blue-500'
    
    // Memory
    const memPct = service.memMaxMb ? (service.memUsedMb / service.memMaxMb) * 100 : 0
    const memColor = memPct > 85 ? 'bg-red-500' : memPct > 70 ? 'bg-orange-500' : 'bg-emerald-500'

    return (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4 flex flex-col transition-all hover:shadow-md relative overflow-hidden">
            {/* Status indicator line at top */}
            <div className={`absolute top-0 left-0 right-0 h-1 ${isUp ? 'bg-emerald-500' : 'bg-red-500'}`} />
            
            {/* Header */}
            <div className="flex justify-between items-start mb-4">
                <div>
                    <div className="flex items-center gap-2">
                        <Server size={14} className={isUp ? 'text-gray-900' : 'text-gray-400'} />
                        <h3 className={`font-semibold text-sm ${isUp ? 'text-gray-900' : 'text-gray-500'}`}>{service.name}</h3>
                    </div>
                    <div className="font-mono text-[9px] text-gray-400 mt-0.5">localhost:{service.port}</div>
                </div>
                <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold font-mono tracking-wider uppercase ring-1 ring-inset ${
                    isUp ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 'bg-red-50 text-red-700 ring-red-600/20'
                }`}>
                    {service.status}
                </span>
            </div>

            {/* Metrics */}
            {isUp ? (
                <div className="space-y-3 flex-1">
                    {/* CPU */}
                    <div>
                        <div className="flex justify-between items-end">
                            <div className="flex items-center gap-1.5 text-[11px] font-medium text-gray-500"><Cpu size={12} /> CPU Usage</div>
                            <div className="font-mono text-[11px] font-semibold text-gray-900">{cpuPct.toFixed(1)}%</div>
                        </div>
                        <ProgressBar percent={cpuPct} colorClass={cpuColor} />
                    </div>

                    {/* RAM */}
                    <div>
                        <div className="flex justify-between items-end">
                            <div className="flex items-center gap-1.5 text-[11px] font-medium text-gray-500"><HardDrive size={12} /> Memory (Heap)</div>
                            <div className="font-mono text-[11px] font-semibold text-gray-900">
                                {service.memUsedMb || 0} <span className="text-gray-400 text-[9px]">/ {service.memMaxMb || 0} MB</span>
                            </div>
                        </div>
                        <ProgressBar percent={memPct} colorClass={memColor} />
                    </div>

                    {/* Stats Grid */}
                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 mt-3">
                        <div>
                            <div className="text-[9px] uppercase tracking-wider font-semibold text-gray-400 mb-0.5">Live Threads</div>
                            <div className="font-mono text-xs font-semibold text-gray-900">{service.threads || 0}</div>
                        </div>
                        <div>
                            <div className="text-[9px] uppercase tracking-wider font-semibold text-gray-400 mb-0.5">HTTP Requests</div>
                            <div className="font-mono text-xs font-semibold text-gray-900">{service.httpRequests?.toLocaleString() || 0}</div>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center py-4 text-gray-400 bg-gray-50/50 rounded-lg border border-gray-100 border-dashed mt-2">
                    <Server size={20} className="mb-2 opacity-50" />
                    <div className="text-[11px] font-medium">Service Offline</div>
                </div>
            )}
        </div>
    )
}

function NativeKafkaViewer() {
    const [status, setStatus] = useState('loading') // loading, offline, online, error
    const [topics, setTopics] = useState([])
    const [selectedTopic, setSelectedTopic] = useState(null)
    const [messages, setMessages] = useState([])
    const [loadingMessages, setLoadingMessages] = useState(false)

    useEffect(() => {
        let isMounted = true;
        const checkCluster = async () => {
            try {
                const res = await fetch('/proxy/kafka-ui/api/clusters')
                if (!res.ok) throw new Error('Kafka UI API unreachable')
                const clusters = await res.json()
                const localCluster = clusters.find(c => c.name === 'zentra-local') || clusters[0]
                
                if (!localCluster) {
                    setStatus('error')
                    return
                }

                if (localCluster.status === 'offline' || localCluster.status === 'OFFLINE') {
                    setStatus('offline')
                    return
                }

                setStatus('online')
                // Fetch topics
                const topicsRes = await fetch(`/proxy/kafka-ui/api/clusters/${localCluster.name}/topics`)
                const topicsData = await topicsRes.json()
                const topicsArray = Array.isArray(topicsData) ? topicsData : (topicsData.topics || [])
                
                if (isMounted) {
                    setTopics(topicsArray.filter(t => !t.name.startsWith('__')))
                }
            } catch (err) {
                if (isMounted) setStatus('error')
            }
        }
        checkCluster()
        return () => { isMounted = false }
    }, [])

    useEffect(() => {
        if (!selectedTopic) return;
        let isMounted = true;
        const fetchMessages = async () => {
            setLoadingMessages(true)
            try {
                const res = await fetch(`/proxy/kafka-ui/api/clusters/zentra-local/topics/${selectedTopic}/messages?limit=50&seekType=BEGINNING`)
                if (res.ok) {
                    const data = await res.json()
                    if (isMounted) setMessages(Array.isArray(data) ? data : (data.messages || []))
                } else {
                    if (isMounted) setMessages([])
                }
            } catch (err) {
                if (isMounted) setMessages([])
            } finally {
                if (isMounted) setLoadingMessages(false)
            }
        }
        fetchMessages()
        const interval = setInterval(fetchMessages, 3000)
        return () => {
            isMounted = false
            clearInterval(interval)
        }
    }, [selectedTopic])

    if (status === 'loading') {
        return (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-50 z-20">
                <RefreshCw className="animate-spin text-gray-400 mb-4" size={32} />
            </div>
        )
    }

    if (status === 'offline' || status === 'error') {
        return (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-gray-50 z-20">
                <AlertTriangle size={48} className="text-orange-500 mb-4" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Kafka Cluster Offline</h3>
                <p className="text-sm text-gray-500 mb-6 max-w-md text-center">
                    The Zentra local Kafka cluster cannot be reached by Kafka UI (No resolvable bootstrap urls). 
                    Ensure Kafka is running and configured correctly in docker-compose.yml.
                </p>
                <div className="p-4 bg-gray-100 rounded-lg border border-gray-200 font-mono text-xs text-gray-600">
                    GET /api/clusters/zentra-local → status: OFFLINE
                </div>
            </div>
        )
    }

    return (
        <div className="absolute inset-0 flex bg-white z-20 overflow-hidden">
            {/* Sidebar: Topics */}
            <div className="w-64 border-r border-gray-200 bg-gray-50/50 flex flex-col">
                <div className="p-4 border-b border-gray-200">
                    <h3 className="text-xs font-mono font-bold text-gray-500 uppercase tracking-widest">Topics</h3>
                </div>
                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {topics.length === 0 ? (
                        <div className="p-4 text-xs text-gray-400 text-center">No topics found</div>
                    ) : (
                        topics.map(t => (
                            <button
                                key={t.name}
                                onClick={() => setSelectedTopic(t.name)}
                                className={`w-full text-left px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                                    selectedTopic === t.name ? 'bg-blue-100 text-blue-700' : 'text-gray-700 hover:bg-gray-100'
                                }`}
                            >
                                <div className="flex items-center gap-2">
                                    <Hash size={14} className={selectedTopic === t.name ? 'text-blue-500' : 'text-gray-400'} />
                                    <span className="truncate">{t.name}</span>
                                </div>
                            </button>
                        ))
                    )}
                </div>
            </div>

            {/* Main Pane: Messages */}
            <div className="flex-1 flex flex-col bg-white">
                {!selectedTopic ? (
                    <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                        <MessageSquare size={48} className="mb-4 opacity-20" />
                        <p>Select a topic to view live messages</p>
                    </div>
                ) : (
                    <>
                        <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-white shadow-sm z-10">
                            <div>
                                <h2 className="text-lg font-semibold text-gray-900">{selectedTopic}</h2>
                                <p className="text-xs text-gray-500 font-mono">Live message stream</p>
                            </div>
                            {loadingMessages && <RefreshCw size={14} className="animate-spin text-gray-400" />}
                        </div>
                        
                        <div className="flex-1 overflow-y-auto p-4 bg-gray-50">
                            {messages.length === 0 ? (
                                <div className="text-center py-12 text-sm text-gray-400">No messages found in this topic</div>
                            ) : (
                                <div className="space-y-3">
                                    {messages.map((m, i) => (
                                        <div key={i} className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
                                            <div className="px-4 py-2 bg-gray-50 border-b border-gray-200 flex justify-between items-center text-xs font-mono text-gray-500">
                                                <div className="flex items-center gap-4">
                                                    <span>P:{m.partition}</span>
                                                    <span>O:{m.offset}</span>
                                                </div>
                                                <span>{new Date(m.timestamp).toLocaleString()}</span>
                                            </div>
                                            <div className="p-4 overflow-x-auto">
                                                <pre className="text-[11px] font-mono text-gray-800">
                                                    {typeof m.content === 'string' ? m.content : JSON.stringify(m.content, null, 2)}
                                                </pre>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>
        </div>
    )
}

export default function InfraHub() {
    const [services, setServices] = useState([])
    const [lastRefresh, setLastRefresh] = useState(new Date())
    const [activeToolId, setActiveToolId] = useState('grafana')

    const activeTool = TOOLS.find(t => t.id === activeToolId)

    useEffect(() => {
        let isMounted = true

        const poll = async () => {
            const [healthData, metricsData] = await Promise.all([
                fetchAllServiceHealth(),
                fetchAllServiceMetrics()
            ])

            if (!isMounted) return

            // Merge health and metrics
            const merged = healthData.map((h, i) => {
                const m = metricsData[i] || {}
                return { ...h, ...m }
            })

            setServices(merged)
            setLastRefresh(new Date())
        }

        poll()
        const interval = setInterval(poll, 5000)
        return () => {
            isMounted = false
            clearInterval(interval)
        }
    }, [])

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-semibold text-gray-900 tracking-tight flex items-center gap-2">
                    <Activity className="text-blue-600" size={24} /> Infrastructure Hub
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                    Native real-time telemetry from Spring Boot Actuator and deep-dive external tools.
                </p>
            </div>

            {/* Grid */}
            <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
                {services.length === 0 ? (
                    // Skeleton loader
                    Array.from({ length: 7 }).map((_, i) => (
                        <div key={i} className="bg-white rounded-xl border border-gray-200 shadow-sm p-4 h-[220px] animate-pulse">
                            <div className="flex justify-between items-start mb-6">
                                <div className="h-3 bg-gray-200 rounded w-20"></div>
                                <div className="h-4 bg-gray-200 rounded w-10"></div>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <div className="h-2 bg-gray-200 rounded w-16 mb-2"></div>
                                    <div className="h-1 bg-gray-100 rounded-full w-full"></div>
                                </div>
                                <div>
                                    <div className="h-2 bg-gray-200 rounded w-20 mb-2"></div>
                                    <div className="h-1 bg-gray-100 rounded-full w-full"></div>
                                </div>
                            </div>
                        </div>
                    ))
                ) : (
                    services.map(s => <ServiceCard key={s.id} service={s} />)
                )}
            </div>
            
            <div className="text-right text-[9px] font-mono text-gray-400 uppercase tracking-widest mt-2 mb-8">
                Last updated: {lastRefresh.toLocaleTimeString()}
            </div>

            <div className="border-t border-gray-200 pt-8 mt-8">
                <h2 className="text-lg font-semibold text-gray-900 tracking-tight mb-4">Deep Dive Tools</h2>
                
                {/* Tool tabs */}
                <div className="flex gap-2 mb-4 overflow-x-auto pb-1 flex-shrink-0">
                    {TOOLS.map(t => {
                        const Icon = t.icon
                        const isActive = t.id === activeToolId
                        return (
                            <button
                                key={t.id}
                                onClick={() => setActiveToolId(t.id)}
                                className={`flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium transition-all whitespace-nowrap border ${
                                    isActive 
                                        ? `border-${t.colorClass}-300 bg-${t.colorClass}-50 text-${t.colorClass}-700 ring-1 ring-${t.colorClass}-100` 
                                        : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
                                }`}
                            >
                                <Icon size={16} className={isActive ? `text-${t.colorClass}-600` : 'text-gray-400'} /> 
                                {t.label}
                                <span className={`font-mono text-[10px] ${isActive ? `text-${t.colorClass}-500` : 'text-gray-400'}`}>:{t.port}</span>
                            </button>
                        )
                    })}
                    <a
                        href={activeTool.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="ml-auto flex items-center gap-1.5 px-3 py-2.5 rounded-lg bg-white border border-gray-200 text-gray-500 hover:text-gray-900 hover:bg-gray-50 text-xs font-medium transition-colors whitespace-nowrap"
                    >
                        <ExternalLink size={14} /> Open in new tab
                    </a>
                </div>

                {/* Tool info bar */}
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-3 rounded-lg bg-gray-50 border border-gray-200 mb-4 text-xs flex-shrink-0 gap-2">
                    <span className="text-gray-600 font-medium">{activeTool.desc}</span>
                    <div className="flex gap-4 items-center">
                        {activeTool.credentials && (
                            <span className="font-mono text-[10px] text-gray-400 uppercase tracking-wider">
                                Login: {activeTool.credentials}
                            </span>
                        )}
                        <span className={`font-mono text-[11px] font-semibold text-${activeTool.colorClass}-600`}>
                            localhost:{activeTool.port}
                        </span>
                    </div>
                </div>

                {/* Iframe or Native Fallback */}
                <div className="flex-1 rounded-xl overflow-hidden border border-gray-200 bg-white shadow-sm relative min-h-[500px]">
                    {activeTool.id === 'kafka-ui' ? (
                        <NativeKafkaViewer />
                    ) : (
                        <>
                            <iframe
                                key={activeTool.id}
                                src={activeTool.embedUrl}
                                className="w-full h-full border-none absolute inset-0 z-10"
                                title={activeTool.label}
                                sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
                                onError={() => { }}
                            />
                            {/* Overlay when not loaded / Loading State Indicator */}
                            <div className="absolute inset-0 flex items-center justify-center bg-gray-50 z-0">
                                <div className="text-center text-gray-400">
                                    <div className="text-4xl mb-3 opacity-30">📡</div>
                                    <div className="text-sm font-medium mb-1.5 text-gray-600">Connecting to {activeTool.label}...</div>
                                    <div className="font-mono text-[10px] text-gray-400 uppercase tracking-widest">Make sure docker compose is running</div>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}
