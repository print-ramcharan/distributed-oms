import { useState, useEffect, useCallback } from 'react'
import { RefreshCw, AlertTriangle, CheckCircle, MessageSquare, ChevronRight, Database, Inbox, ExternalLink, Search } from 'lucide-react'

const CLUSTER = 'zentra-local'
const KAFKA_API = `/proxy/kafka-ui/api/clusters/${CLUSTER}`

const TOPIC_META = {
    'order.event.created':            { label: 'Order Created',        color: 'blue',    category: 'event' },
    'order.event.progress-updated':   { label: 'Order Progress',       color: 'blue',    category: 'event' },
    'order.command.advance-progress': { label: 'Advance Progress Cmd', color: 'purple',  category: 'command' },
    'order.command.dlq':              { label: 'Order DLQ ⚠',          color: 'red',     category: 'dlq' },
    'payment.initiate':               { label: 'Payment Initiate',     color: 'green',   category: 'command' },
    'payment.completed':              { label: 'Payment Completed',    color: 'green',   category: 'event' },
    'payment.failed':                 { label: 'Payment Failed',       color: 'red',     category: 'event' },
    'payment.refund.command':         { label: 'Refund Command',       color: 'orange',  category: 'command' },
    'payment.refunded':               { label: 'Payment Refunded',     color: 'orange',  category: 'event' },
    'inventory.reserve.command':      { label: 'Reserve Inventory Cmd', color: 'teal',   category: 'command' },
    'inventory.reserved':             { label: 'Inventory Reserved',   color: 'teal',    category: 'event' },
    'inventory.unavailable':          { label: 'Inventory Unavailable', color: 'red',    category: 'event' },
    'fulfillment.initiated':          { label: 'Fulfillment Initiated', color: 'purple', category: 'event' },
}

const COLOR_MAP = {
    blue:   { bg: 'bg-blue-50',   text: 'text-blue-700',   border: 'border-blue-200',  dot: 'bg-blue-400' },
    green:  { bg: 'bg-green-50',  text: 'text-green-700',  border: 'border-green-200', dot: 'bg-green-400' },
    red:    { bg: 'bg-red-50',    text: 'text-red-700',    border: 'border-red-200',   dot: 'bg-red-400' },
    orange: { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-orange-200',dot: 'bg-orange-400' },
    purple: { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200',dot: 'bg-purple-400' },
    teal:   { bg: 'bg-teal-50',   text: 'text-teal-700',   border: 'border-teal-200',  dot: 'bg-teal-400' },
    gray:   { bg: 'bg-gray-50',   text: 'text-gray-600',   border: 'border-gray-200',  dot: 'bg-gray-400' },
}

async function kafkaGet(path) {
    const res = await fetch(`${KAFKA_API}${path}`)
    if (!res.ok) throw new Error(`Kafka UI API ${res.status}`)
    return res.json()
}

export default function DlqViewer() {
    const [topics, setTopics] = useState([])
    const [selectedTopic, setSelectedTopic] = useState(null)
    const [messages, setMessages] = useState([])
    const [loading, setLoading] = useState(true)
    const [msgLoading, setMsgLoading] = useState(false)
    const [error, setError] = useState(null)
    const [filter, setFilter] = useState('all') // all | dlq | event | command
    const [search, setSearch] = useState('')
    const [selectedMsg, setSelectedMsg] = useState(null)
    const [lastRefresh, setLastRefresh] = useState(null)

    const loadTopics = useCallback(async () => {
        setLoading(true)
        setError(null)
        try {
            const data = await kafkaGet('/topics')
            const enriched = (data.topics || []).map(t => ({
                ...t,
                meta: TOPIC_META[t.name] || { label: t.name, color: 'gray', category: 'other' },
                totalMessages: t.partitions?.reduce((sum, p) => sum + (p.offsetMax - p.offsetMin), 0) ?? 0,
            }))
            setTopics(enriched.sort((a, b) => {
                if (a.meta.category === 'dlq') return -1
                if (b.meta.category === 'dlq') return 1
                return b.totalMessages - a.totalMessages
            }))
            setLastRefresh(new Date())
        } catch (e) {
            setError('Cannot reach Kafka UI. Make sure it is running on port 8090.')
        } finally {
            setLoading(false)
        }
    }, [])

    const loadMessages = useCallback(async (topicName) => {
        setMsgLoading(true)
        setMessages([])
        setSelectedMsg(null)
        try {
            const response = await fetch(`${KAFKA_API}/topics/${topicName}/messages?limit=50`)
            
            if (!response.ok) throw new Error(`Kafka UI API ${response.status}`)
            
            const reader = response.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''
            const collectedMessages = []

            while (true) {
                const { value, done } = await reader.read()
                if (done) break
                
                buffer += decoder.decode(value, { stream: true })
                const lines = buffer.split('\n')
                buffer = lines.pop()

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        try {
                            const json = JSON.parse(line.substring(5))
                            if (json.type === 'MESSAGE' && json.message) {
                                collectedMessages.push(json.message)
                                setMessages([...collectedMessages])
                            }
                            if (json.type === 'DONE') break
                        } catch (e) {
                            console.error('Error parsing Kafka message line', e)
                        }
                    }
                }
            }
        } catch (e) {
            console.error('Failed to load messages', e)
        } finally {
            setMsgLoading(false)
        }
    }, [])

    useEffect(() => { loadTopics() }, [loadTopics])

    useEffect(() => {
        if (selectedTopic) loadMessages(selectedTopic.name)
    }, [selectedTopic, loadMessages])

    const filteredTopics = topics.filter(t => {
        if (filter !== 'all' && t.meta.category !== filter) return false
        if (search && !t.name.toLowerCase().includes(search.toLowerCase())) return false
        return true
    })

    const totalMessages = topics.reduce((s, t) => s + t.totalMessages, 0)
    const dlqTopics = topics.filter(t => t.meta.category === 'dlq')
    const dlqMessages = dlqTopics.reduce((s, t) => s + t.totalMessages, 0)

    return (
        <div className="h-full flex flex-col">
            {/* Header */}
            <div className="px-8 py-6 border-b border-gray-100">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold text-gray-900 tracking-tight">Kafka Topic Monitor</h1>
                        <p className="text-sm text-gray-500 mt-0.5">
                            Live topic stats from <span className="font-mono text-xs bg-gray-100 px-1.5 py-0.5 rounded">{CLUSTER}</span>
                            {lastRefresh && <span className="ml-2 text-gray-400">· refreshed {lastRefresh.toLocaleTimeString()}</span>}
                        </p>
                    </div>
                    <div className="flex items-center gap-3">
                        <a href="http://localhost:8090" target="_blank" rel="noreferrer"
                            className="flex items-center gap-1.5 text-xs text-blue-600 hover:text-blue-800 font-medium">
                            Open Kafka UI <ExternalLink size={12} />
                        </a>
                        <button onClick={loadTopics}
                            className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-900 border border-gray-200 rounded-lg px-3 py-1.5 bg-white hover:bg-gray-50 transition-all">
                            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Refresh
                        </button>
                    </div>
                </div>

                {/* Summary Stats */}
                <div className="flex gap-4 mt-4">
                    <StatPill icon={<Database size={13} />} label="Topics" value={topics.length} color="blue" />
                    <StatPill icon={<MessageSquare size={13} />} label="Total Messages" value={totalMessages.toLocaleString()} color="gray" />
                    <StatPill icon={<AlertTriangle size={13} />} label="DLQ Messages" value={dlqMessages} color={dlqMessages > 0 ? 'red' : 'green'} />
                    <StatPill icon={<CheckCircle size={13} />} label="Active Topics" value={topics.filter(t => t.totalMessages > 0).length} color="green" />
                </div>
            </div>

            {error && (
                <div className="mx-8 mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center gap-2">
                    <AlertTriangle size={16} /> {error}
                </div>
            )}

            <div className="flex flex-1 overflow-hidden">
                {/* Topic List */}
                <div className="w-96 border-r border-gray-100 flex flex-col overflow-hidden">
                    {/* Filter Bar */}
                    <div className="px-4 py-3 border-b border-gray-100 space-y-2">
                        <div className="relative">
                            <Search size={13} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input value={search} onChange={e => setSearch(e.target.value)}
                                placeholder="Search topics..."
                                className="w-full pl-8 pr-3 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:border-blue-300" />
                        </div>
                        <div className="flex gap-1">
                            {[['all', 'All'], ['dlq', 'DLQ'], ['event', 'Events'], ['command', 'Commands']].map(([val, lbl]) => (
                                <button key={val} onClick={() => setFilter(val)}
                                    className={`flex-1 text-[10px] font-semibold py-1 rounded-md transition-all ${
                                        filter === val ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                    }`}>{lbl}</button>
                            ))}
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto">
                        {loading ? (
                            <div className="flex items-center justify-center py-16 text-gray-400 text-sm">
                                <RefreshCw size={16} className="animate-spin mr-2" /> Loading...
                            </div>
                        ) : filteredTopics.length === 0 ? (
                            <div className="text-center py-12 text-gray-400 text-sm">No topics match</div>
                        ) : (
                            filteredTopics.map(topic => {
                                const c = COLOR_MAP[topic.meta.color] || COLOR_MAP.gray
                                const isSelected = selectedTopic?.name === topic.name
                                return (
                                    <button key={topic.name} onClick={() => setSelectedTopic(topic)}
                                        className={`w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 transition-all ${isSelected ? 'bg-blue-50 border-l-2 border-l-blue-500' : ''}`}>
                                        <div className="flex items-center justify-between gap-2">
                                            <div className="flex items-center gap-2 min-w-0">
                                                <span className={`w-2 h-2 rounded-full flex-shrink-0 ${c.dot}`} />
                                                <div className="min-w-0">
                                                    <div className={`text-xs font-semibold truncate ${isSelected ? 'text-blue-700' : 'text-gray-800'}`}>
                                                        {topic.meta.label}
                                                    </div>
                                                    <div className="text-[10px] font-mono text-gray-400 truncate">{topic.name}</div>
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-2 flex-shrink-0">
                                                <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${topic.totalMessages > 0 ? `${c.bg} ${c.text}` : 'bg-gray-100 text-gray-400'}`}>
                                                    {topic.totalMessages.toLocaleString()}
                                                </span>
                                                <ChevronRight size={12} className="text-gray-300" />
                                            </div>
                                        </div>
                                    </button>
                                )
                            })
                        )}
                    </div>
                </div>

                {/* Message Panel */}
                <div className="flex-1 overflow-hidden flex flex-col">
                    {!selectedTopic ? (
                        <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                            <Inbox size={40} strokeWidth={1} className="mb-3" />
                            <div className="text-sm font-medium">Select a topic to inspect messages</div>
                            <div className="text-xs mt-1">Shows the 50 most recent messages</div>
                        </div>
                    ) : (
                        <>
                            <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
                                <div>
                                    <div className="text-sm font-bold text-gray-900">{selectedTopic.meta.label}</div>
                                    <div className="text-xs font-mono text-gray-400">{selectedTopic.name} · {selectedTopic.totalMessages.toLocaleString()} total messages</div>
                                </div>
                                <button onClick={() => loadMessages(selectedTopic.name)}
                                    className="p-1.5 text-gray-400 hover:text-blue-600 rounded-lg hover:bg-blue-50 transition-all">
                                    <RefreshCw size={14} className={msgLoading ? 'animate-spin' : ''} />
                                </button>
                            </div>

                            <div className="flex flex-1 overflow-hidden">
                                {/* Message list */}
                                <div className="w-64 border-r border-gray-100 overflow-y-auto">
                                    {msgLoading ? (
                                        <div className="flex items-center justify-center py-8 text-gray-400 text-xs">
                                            <RefreshCw size={13} className="animate-spin mr-1.5" /> Loading...
                                        </div>
                                    ) : messages.length === 0 ? (
                                        <div className="text-center py-8 text-gray-400 text-xs">No messages yet</div>
                                    ) : (
                                        messages.map((msg, i) => (
                                            <button key={i} onClick={() => setSelectedMsg(msg)}
                                                className={`w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 transition-all ${selectedMsg === msg ? 'bg-blue-50' : ''}`}>
                                                <div className="text-[10px] font-mono text-gray-500 mb-0.5">Offset {msg.offset} · Part {msg.partition}</div>
                                                <div className="text-xs text-gray-700 truncate font-mono">
                                                    {msg.key || <span className="text-gray-400 italic">null key</span>}
                                                </div>
                                                <div className="text-[10px] text-gray-400 mt-0.5">
                                                    {msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString() : '—'}
                                                </div>
                                            </button>
                                        ))
                                    )}
                                </div>

                                {/* Message detail */}
                                <div className="flex-1 overflow-y-auto p-5">
                                    {!selectedMsg ? (
                                        <div className="flex items-center justify-center h-full text-gray-400 text-xs">
                                            Select a message to inspect its payload
                                        </div>
                                    ) : (
                                        <div className="space-y-4">
                                            <div className="grid grid-cols-3 gap-3">
                                                {[
                                                    ['Offset', selectedMsg.offset],
                                                    ['Partition', selectedMsg.partition],
                                                    ['Timestamp', selectedMsg.timestamp ? new Date(selectedMsg.timestamp).toLocaleString() : '—'],
                                                ].map(([k, v]) => (
                                                    <div key={k} className="bg-gray-50 rounded-lg p-3 border border-gray-100">
                                                        <div className="text-[10px] text-gray-400 uppercase font-bold mb-1">{k}</div>
                                                        <div className="text-xs font-mono text-gray-800 break-all">{v}</div>
                                                    </div>
                                                ))}
                                            </div>
                                            {selectedMsg.key && (
                                                <div>
                                                    <div className="text-[10px] text-gray-400 uppercase font-bold mb-1">Key</div>
                                                    <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 font-mono text-xs text-gray-700 break-all">{selectedMsg.key}</div>
                                                </div>
                                            )}
                                            <div>
                                                <div className="text-[10px] text-gray-400 uppercase font-bold mb-1">Payload</div>
                                                <pre className="bg-gray-900 text-green-400 rounded-lg p-4 text-xs overflow-x-auto font-mono leading-relaxed whitespace-pre-wrap break-all">
                                                    {(() => {
                                                        try { return JSON.stringify(JSON.parse(selectedMsg.content || 'null'), null, 2) }
                                                        catch { return selectedMsg.content || '(empty)' }
                                                    })()}
                                                </pre>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}

function StatPill({ icon, label, value, color }) {
    const colors = {
        blue:  'bg-blue-50 text-blue-700 border-blue-200',
        green: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        red:   'bg-red-50 text-red-700 border-red-200',
        gray:  'bg-gray-50 text-gray-600 border-gray-200',
    }
    return (
        <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full border text-xs font-medium ${colors[color] || colors.gray}`}>
            {icon}
            <span className="text-[10px] opacity-70">{label}</span>
            <span className="font-bold">{value}</span>
        </div>
    )
}
