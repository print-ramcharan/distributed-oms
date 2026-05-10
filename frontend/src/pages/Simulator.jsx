import { useState, useEffect } from 'react'
import { ShoppingCart, CreditCard, Package, Truck, Bell, CheckCircle, XCircle, Loader, AlertTriangle, ChevronRight, Plus, RefreshCw, Database, Info } from 'lucide-react'
import { toast } from 'sonner'
import { useSagaStore } from '../store.js'
import { createOrder, fetchSaga, fetchOrders, toggleKillSwitch, fetchInventory, createInventoryItem } from '../api.js'

const STEP_ICONS = { ShoppingCart, CreditCard, Package, Truck, Bell }

const STEP_COLORS = {
    pending: 'text-gray-400 border-gray-200 bg-gray-50',
    processing: 'text-blue-600 border-blue-500 bg-blue-50 shadow-blue-500/20',
    completed: 'text-emerald-500 border-emerald-500 bg-emerald-50 shadow-emerald-500/20',
    failed: 'text-red-500 border-red-500 bg-red-50 shadow-red-500/20',
    compensating: 'text-orange-500 border-orange-500 bg-orange-50 shadow-orange-500/20',
}

const DEFAULT_PRODUCTS = [
    { id: 'PROD-MAC-001', name: 'MacBook Pro 16"', price: 249900, category: 'Electronics', initialStock: 12 },
    { id: 'PROD-IPH-002', name: 'iPhone 16 Pro', price: 134900, category: 'Electronics', initialStock: 48 },
    { id: 'PROD-SNY-003', name: 'Sony WH-1000XM5', price: 29990, category: 'Audio', initialStock: 5 },
    { id: 'PROD-SAM-004', name: 'Samsung 4K Monitor', price: 54999, category: 'Display', initialStock: 8 },
]

function SagaNode({ step }) {
    const Icon = STEP_ICONS[step.icon]
    const statusClasses = STEP_COLORS[step.status]
    const isProcessing = step.status === 'processing'
    const isFailed = step.status === 'failed'
    const isCompensating = step.status === 'compensating'
    const isCompleted = step.status === 'completed'

    return (
        <div className="flex flex-col items-center gap-2 flex-1 relative">
            <div
                className={`w-12 h-12 rounded-full border-2 flex items-center justify-center transition-all duration-300 relative ${statusClasses} ${step.status !== 'pending' ? 'shadow-lg' : ''}`}
            >
                {isProcessing && (
                    <div className="absolute -inset-1.5 rounded-full border-2 border-blue-400/40 animate-ping" />
                )}
                {isFailed ? <XCircle size={20} /> :
                 isCompensating ? <AlertTriangle size={20} /> :
                 isCompleted ? <CheckCircle size={20} /> :
                 <Icon size={18} />}
            </div>
            <div className="text-center">
                <div className="text-[11px] font-semibold text-gray-900 mb-0.5">{step.label}</div>
                <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold font-mono tracking-wider uppercase ring-1 ring-inset ${
                    isCompleted ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 
                    isFailed ? 'bg-red-50 text-red-700 ring-red-600/20' : 
                    isCompensating ? 'bg-orange-50 text-orange-700 ring-orange-600/20' : 
                    isProcessing ? 'bg-blue-50 text-blue-700 ring-blue-600/20' : 
                    'bg-gray-50 text-gray-500 ring-gray-400/20'
                }`}>
                    {step.status}
                </span>
            </div>
        </div>
    )
}

function ConnectorArrow({ fromStep, reversed }) {
    const isActive = fromStep.status === 'completed' || fromStep.status === 'compensating' || fromStep.status === 'failed'
    const colorClass = reversed ? 'text-orange-500' : 'text-blue-500'
    return (
        <div className="flex items-center mb-8 flex-[0.5] justify-center">
            {reversed ? (
                <ChevronRight size={18} className={`rotate-180 ${colorClass} ${isActive ? 'opacity-100' : 'opacity-20'}`} />
            ) : (
                <ChevronRight size={18} className={`${colorClass} ${isActive ? 'opacity-100' : 'opacity-20'}`} />
            )}
        </div>
    )
}

function SagaVisualizer({ order }) {
    if (!order) return null
    const hasCompensation = order.steps.some(s => s.status === 'compensating' || s.status === 'failed')

    return (
        <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6 mt-6">
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h3 className="text-sm font-semibold text-gray-900">Saga Transaction Visualizer</h3>
                    <div className="font-mono text-xs text-blue-600 font-semibold mt-0.5">{order.id}</div>
                </div>
                <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold font-mono uppercase ring-1 ring-inset ${
                    order.status === 'CONFIRMED' ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 
                    order.status === 'CANCELLED' ? 'bg-red-50 text-red-700 ring-red-600/20' : 
                    order.status === 'COMPENSATING' ? 'bg-orange-50 text-orange-700 ring-orange-600/20' : 
                    'bg-blue-50 text-blue-700 ring-blue-600/20'
                }`}>
                    {order.status}
                </span>
            </div>

            {order.reason && (
                <div className={`border rounded-lg p-3 mb-5 flex items-start gap-2 ${hasCompensation ? 'bg-orange-50 border-orange-200' : 'bg-red-50 border-red-200'}`}>
                    {hasCompensation ? <AlertTriangle size={16} className="text-orange-500 mt-0.5 shrink-0" /> : <XCircle size={16} className="text-red-500 mt-0.5 shrink-0" />}
                    <div>
                        <div className={`text-[11px] font-bold uppercase tracking-wider ${hasCompensation ? 'text-orange-800' : 'text-red-800'}`}>
                            {hasCompensation ? 'Saga Compensation Triggered' : 'Saga Failed'}
                        </div>
                        <div className={`text-[12px] font-mono mt-0.5 ${hasCompensation ? 'text-orange-700' : 'text-red-700'}`}>
                            Cause: {order.reason}
                        </div>
                    </div>
                </div>
            )}

            <div className="flex items-center justify-between mb-8">
                {order.steps.map((step, idx) => (
                    <div key={step.id} className="contents">
                        <SagaNode step={step} />
                        {idx < order.steps.length - 1 && (
                            <ConnectorArrow 
                                fromStep={step} 
                                reversed={step.status === 'compensating' || (step.status === 'failed' && idx > 0)} 
                            />
                        )}
                    </div>
                ))}
            </div>

            <div className="mt-6 pt-5 border-t border-gray-100">
                <div className="flex items-center gap-2 mb-3">
                    <Info size={14} className="text-gray-400" />
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Execution Timeline</span>
                </div>
                <div className="space-y-2">
                    {order.steps.filter(s => s.status !== 'pending').map(step => (
                        <div key={step.id} className="flex justify-between items-center py-1.5 px-3 rounded-lg bg-gray-50/50 text-[11px] font-mono border border-gray-100/50">
                            <span className="text-gray-500 font-medium">{step.label}</span>
                            <div className="flex items-center gap-3">
                                <span className={`font-bold px-1.5 py-0.5 rounded ${
                                    step.status === 'completed' ? 'bg-emerald-100 text-emerald-700' :
                                    step.status === 'failed' ? 'bg-red-100 text-red-700' :
                                    step.status === 'compensating' ? 'bg-orange-100 text-orange-700' :
                                    'bg-blue-100 text-blue-700'
                                }`}>
                                    {step.status.toUpperCase()}
                                </span>
                                <span className="text-gray-400">
                                    {step.completedAt ? step.completedAt.toLocaleTimeString() : '...'}
                                </span>
                            </div>
                        </div>
                    ))}
                    {order.status === 'COMPENSATING' && (
                        <div className="animate-pulse flex items-center justify-center py-2 gap-2 text-orange-600 bg-orange-50/50 rounded-lg border border-orange-100 border-dashed">
                            <RefreshCw size={12} className="animate-spin" />
                            <span className="text-[10px] font-bold uppercase tracking-wider italic">Executing rollback logic...</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

const mapSagaToVisualizer = (sagaData) => {
    if (!sagaData) return null;
    
    const s = sagaData.state;
    const r = sagaData.reason || "";
    const isCompensating = s === 'COMPENSATING';
    const isCompensated = s === 'COMPENSATED';
    const isFullyFailed = s === 'FAILED';
    const isInventoryFailure = r.includes("Inventory") || r.includes("STOCK") || r.includes("PRODUCT_NOT_FOUND");
    const isPaymentFailure = r.includes("Payment") || r.includes("balance") || r.includes("killed");

    // Payment Step Status
    const pStatus = (s === 'PAYMENT_FAILED' || (isFullyFailed && isPaymentFailure)) ? 'failed' : 
                   ['PAYMENT_COMPLETED', 'INVENTORY_REQUESTED', 'INVENTORY_RESERVED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED'].includes(s) 
                   ? (isCompensating || isCompensated || isFullyFailed ? 'compensating' : 'completed') 
                   : s === 'PAYMENT_REQUESTED' ? 'processing' : 'pending';
                   
    // Inventory Step Status
    const iStatus = (s === 'INVENTORY_FAILED' || (isCompensating && isInventoryFailure) || (isCompensated && isInventoryFailure)) ? 'failed' :
                   ['INVENTORY_RESERVED', 'COMPLETED'].includes(s) ? (isCompensating || isCompensated ? 'compensating' : 'completed') :
                   s === 'INVENTORY_REQUESTED' ? 'processing' : 'pending';
                   
    // Fulfillment Step Status
    const fStatus = s === 'COMPLETED' ? 'completed' : 
                   ['INVENTORY_RESERVED'].includes(s) && !isCompensating && !isCompensated ? 'processing' : 'pending';

    // Notification Step Status
    const nStatus = s === 'COMPLETED' ? 'completed' : 'pending';

    const overall = s === 'COMPLETED' ? 'CONFIRMED' : 
                    isCompensating ? 'COMPENSATING' : 
                    (isCompensated || isFullyFailed) ? 'CANCELLED' : 'PROCESSING';

    const time = new Date(sagaData.createdAt);

    return {
        id: sagaData.orderId,
        status: overall,
        reason: sagaData.reason,
        steps: [
            { id: 'order_created', label: 'Order Created', status: 'completed', icon: 'ShoppingCart', startedAt: time, completedAt: time },
            { id: 'payment', label: 'Payment Service', status: pStatus, icon: 'CreditCard', startedAt: pStatus !== 'pending' ? time : null, completedAt: (pStatus === 'completed' || pStatus === 'compensating') ? time : null },
            { id: 'inventory', label: 'Inventory Service', status: iStatus, icon: 'Package', startedAt: iStatus !== 'pending' ? time : null, completedAt: iStatus === 'completed' ? time : null },
            { id: 'fulfillment', label: 'Fulfillment Service', status: fStatus, icon: 'Truck', startedAt: fStatus !== 'pending' ? time : null, completedAt: fStatus === 'completed' ? time : null },
            { id: 'notification', label: 'Notification Service', status: nStatus, icon: 'Bell', startedAt: nStatus !== 'pending' ? time : null, completedAt: nStatus === 'completed' ? time : null },
        ]
    }
}

export default function Simulator() {
    const [products, setProducts] = useState([])
    const [selected, setSelected] = useState(null)
    const [scenario, setScenario] = useState('success')
    const { activeOrderId, setActiveOrderId } = useSagaStore()
    
    const [activeSagaData, setActiveSagaData] = useState(null)
    const [recentOrders, setRecentOrders] = useState([])
    const [isPlacingOrder, setIsPlacingOrder] = useState(false)
    const [isLoadingProducts, setIsLoadingProducts] = useState(true)

    const [newProdId, setNewProdId] = useState('')
    const [newProdStock, setNewProdStock] = useState(10)
    const [isSeeding, setIsSeeding] = useState(false)

    const loadInventory = async () => {
        setIsLoadingProducts(true)
        try {
            const data = await fetchInventory()
            const enhanced = data.map(item => {
                const def = DEFAULT_PRODUCTS.find(p => p.id === item.productId)
                return {
                    id: item.productId,
                    name: def ? def.name : `Product ${item.productId.substring(0, 4)}`,
                    price: def ? def.price : 1000,
                    category: def ? def.category : 'General',
                    stock: item.availableQuantity
                }
            })
            setProducts(enhanced)
        } catch (e) {
            console.error("Failed to load inventory", e)
        } finally {
            setIsLoadingProducts(false)
        }
    }

    const handleAddProduct = async (e) => {
        if (e) e.preventDefault()
        if (!newProdId) return
        try {
            await createInventoryItem(newProdId, newProdStock)
            toast.success(`Product ${newProdId} added to Inventory!`)
            setNewProdId('')
            loadInventory()
        } catch (e) {
            toast.error("Failed to add product.")
        }
    }

    const handleSeedData = async () => {
        setIsSeeding(true)
        try {
            for (const p of DEFAULT_PRODUCTS) {
                try { await createInventoryItem(p.id, p.initialStock) } catch (e) {}
            }
            toast.success("Inventory seeded!")
            loadInventory()
        } finally {
            setIsSeeding(false)
        }
    }

    useEffect(() => {
        loadInventory()
    }, [])

    useEffect(() => {
        let isMounted = true;
        const poll = async () => {
            if (activeOrderId && isMounted) {
                try {
                    const saga = await fetchSaga(activeOrderId)
                    if (saga && isMounted) {
                        setActiveSagaData(mapSagaToVisualizer(saga))
                    }
                } catch (e) {}
            }
            try {
                const orders = await fetchOrders(5)
                if (isMounted && Array.isArray(orders)) {
                    setRecentOrders(orders)
                }
            } catch (e) {}
        }
        poll()
        const interval = setInterval(poll, 1500)
        return () => { isMounted = false; clearInterval(interval) }
    }, [activeOrderId])

    const handleOrder = async () => {
        if (!selected) return
        setIsPlacingOrder(true)
        
        // RESET: Clear old state immediately so the UI starts fresh
        setActiveSagaData(null)
        setActiveOrderId(null)
        
        try {
            if (scenario === 'payment_failure') await toggleKillSwitch('payment', true)
            const quantity = scenario === 'inventory_failure' ? 9999 : 1
            const items = [{ productId: selected.id, quantity, price: selected.price }]
            const response = await createOrder("simulator@zentra.com", items)
            
            if (!response?.orderId) throw new Error("Backend connection failed.")
            
            toast.success(`Order #${response.orderId.substring(0,8)} placed successfully!`)
            
            // Set the new ID - Polling will take over from here using real DB states
            setActiveOrderId(response.orderId)
            
            if (scenario === 'payment_failure') {
                setTimeout(() => {
                    toggleKillSwitch('payment', false)
                    toast.info("⚡ Chaos Recovered: Payment Service is back online.")
                }, 5000)
            }
        } catch (error) {
            toast.error(error.message || "Failed to place order.")
        } finally {
            setIsPlacingOrder(false)
        }
    }

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
            <div className="flex justify-between items-start">
                <div>
                    <h1 className="text-2xl font-semibold text-gray-900 tracking-tight">Order Simulator</h1>
                    <p className="text-sm text-gray-500 mt-1">
                        Place an order and watch the actual Saga pattern execute across microservices.
                    </p>
                </div>
                <button 
                    onClick={loadInventory}
                    className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                >
                    <RefreshCw size={18} className={isLoadingProducts ? 'animate-spin' : ''} />
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
                <div className="lg:col-span-2 space-y-5">
                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <h3 className="text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-widest mb-4">Select Product (From DB)</h3>
                        {products.length === 0 && !isLoadingProducts ? (
                            <div className="text-center py-6 border-2 border-dashed border-gray-100 rounded-lg">
                                <p className="text-xs text-gray-400 mb-4">Inventory is empty!</p>
                                <button onClick={handleSeedData} className="inline-flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white text-[11px] font-bold rounded-lg" disabled={isSeeding}>
                                    <Database size={12} /> {isSeeding ? 'Seeding...' : 'Seed Default Products'}
                                </button>
                            </div>
                        ) : (
                            <div className="space-y-2 max-h-[250px] overflow-y-auto pr-1">
                                {products.map(p => (
                                    <div key={p.id} onClick={() => setSelected(p)} className={`p-3.5 rounded-lg cursor-pointer border transition-all ${selected?.id === p.id ? 'border-blue-400 bg-blue-50 ring-1 ring-blue-100' : 'border-gray-200 bg-white hover:bg-gray-50'}`}>
                                        <div className="flex justify-between items-center">
                                            <div>
                                                <div className={`text-sm font-semibold ${selected?.id === p.id ? 'text-blue-900' : 'text-gray-900'}`}>{p.name}</div>
                                                <div className={`text-[11px] mt-0.5 ${selected?.id === p.id ? 'text-blue-600' : 'text-gray-500'}`}>{p.id} · {p.stock} in stock</div>
                                            </div>
                                            <div className={`font-mono text-sm font-bold ${selected?.id === p.id ? 'text-blue-700' : 'text-gray-700'}`}>₹{p.price.toLocaleString()}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <h3 className="text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-widest mb-4">Inventory Manager</h3>
                        <form onSubmit={handleAddProduct} className="flex gap-2">
                            <input type="text" placeholder="Product ID (e.g. p1)" value={newProdId} onChange={e => setNewProdId(e.target.value)} className="flex-1 px-3 py-2 text-xs border border-gray-200 rounded-lg outline-none" />
                            <input type="number" value={newProdStock} onChange={e => setNewProdStock(parseInt(e.target.value))} className="w-16 px-2 py-2 text-xs border border-gray-200 rounded-lg outline-none" />
                            <button className="p-2 bg-gray-900 text-white rounded-lg hover:bg-black transition-colors"><Plus size={16} /></button>
                        </form>
                    </div>

                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <h3 className="text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-widest mb-4">Test Scenario</h3>
                        <div className="grid grid-cols-1 gap-2">
                            {[
                                { id: 'success', label: 'Happy Path', desc: 'All steps succeed', colorClass: 'emerald' },
                                { id: 'payment_failure', label: 'Payment Failure', desc: 'Kills payment service briefly', colorClass: 'red' },
                                { id: 'inventory_failure', label: 'Out of Stock', desc: 'Orders 9,999 units (Compensate)', colorClass: 'orange' },
                            ].map(s => (
                                <div key={s.id} onClick={() => setScenario(s.id)} className={`p-3 rounded-lg cursor-pointer border transition-all flex justify-between items-center ${scenario === s.id ? `border-${s.colorClass}-300 bg-${s.colorClass}-50 ring-1 ring-${s.colorClass}-100` : 'border-gray-200 bg-white hover:bg-gray-50'}`}>
                                    <div>
                                        <div className={`text-[13px] font-semibold ${scenario === s.id ? `text-${s.colorClass}-800` : 'text-gray-900'}`}>{s.label}</div>
                                        <div className={`text-[10px] mt-0.5 ${scenario === s.id ? `text-${s.colorClass}-600` : 'text-gray-500'}`}>{s.desc}</div>
                                    </div>
                                    {scenario === s.id && <CheckCircle size={16} className={`text-${s.colorClass}-600`} />}
                                </div>
                            ))}
                        </div>
                    </div>

                    <button className="w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-xl text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 shadow-sm disabled:opacity-50 transition-all" onClick={handleOrder} disabled={!selected || isPlacingOrder}>
                        {isPlacingOrder ? <Loader className="animate-spin" size={16} /> : <ShoppingCart size={16} />}
                        {isPlacingOrder ? 'Sending to API...' : `Place Order — ₹${selected?.price.toLocaleString() || '0'}`}
                    </button>
                </div>

                <div className="lg:col-span-3">
                    {activeSagaData ? <SagaVisualizer order={activeSagaData} /> : (
                        <div className="bg-white border border-gray-200 border-dashed rounded-xl p-16 text-center">
                            <ShoppingCart size={48} className="text-gray-200 mx-auto mb-4" />
                            <div className="text-gray-500 text-sm font-medium">Select a product and click "Place Order"<br />to watch the Saga execute in real-time</div>
                        </div>
                    )}

                    {recentOrders.length > 0 && (
                        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm mt-6">
                            <div className="text-[10px] font-mono font-semibold text-gray-400 uppercase tracking-widest mb-3">Previous Orders (Real-time DB)</div>
                            <div className="space-y-1">
                                {recentOrders.slice(0, 5).map(o => (
                                    <div key={o.orderId} className="flex justify-between items-center py-2 border-b border-gray-100 last:border-0">
                                        <div>
                                            <span className="font-semibold text-[13px] text-gray-900">{products.find(p => p.id === o.productId)?.name || o.productId}</span>
                                            <span className="ml-2 text-[10px] text-gray-400 font-mono tracking-wide">#{o.orderId.substring(0, 8).toUpperCase()}</span>
                                        </div>
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-[9px] font-bold font-mono uppercase ring-1 ring-inset ${o.status === 'COMPLETED' || o.status === 'CONFIRMED' ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : o.status === 'CANCELLED' || o.status === 'FAILED' ? 'bg-red-50 text-red-700 ring-red-600/20' : o.status === 'COMPENSATING' ? 'bg-orange-50 text-orange-700 ring-orange-600/20' : 'bg-blue-50 text-blue-700 ring-blue-600/20'}`}>{o.status}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
