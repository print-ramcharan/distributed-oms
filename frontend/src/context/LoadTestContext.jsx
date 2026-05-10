import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react'
import { fetchInventory } from '../api'

const LoadTestContext = createContext()

export function useLoadTest() {
    return useContext(LoadTestContext)
}

export function LoadTestProvider({ children }) {
    const [isRunning, setIsRunning] = useState(false)
    const [products, setProducts] = useState([])
    const [isLoadingProducts, setIsLoadingProducts] = useState(false)
    const [config, setConfig] = useState({
        rps: 5,
        scenarios: { happy: 60, out_of_stock: 20, invalid_product: 10, read_orders: 10 }
    })
    const [metrics, setMetrics] = useState({
        totalRequests: 0, successCount: 0, errorCount: 0,
        currentRps: 0, p50: 0, p95: 0, p99: 0,
        byScenario: { happy: { success: 0, error: 0 }, out_of_stock: { success: 0, error: 0 }, invalid_product: { success: 0, error: 0 }, read_orders: { success: 0, error: 0 } },
        successRate: 100, rpsHistory: []
    })

    const runnerRef = useRef(null)
    const statsRef = useRef({
        latencies: [], requestsThisSecond: 0, success: 0, errors: 0, total: 0,
        byScenario: { happy: { success: 0, error: 0 }, out_of_stock: { success: 0, error: 0 }, invalid_product: { success: 0, error: 0 }, read_orders: { success: 0, error: 0 } }
    })
    const configRef = useRef(config)
    const productsRef = useRef(products)

    useEffect(() => {
        configRef.current = config
    }, [config])

    useEffect(() => {
        productsRef.current = products
    }, [products])

    const loadProducts = useCallback(async () => {
        setIsLoadingProducts(true)
        try {
            const data = await fetchInventory()
            setProducts(data || [])
        } catch (e) {
            console.error('Failed to load products', e)
        } finally {
            setIsLoadingProducts(false)
        }
    }, [])

    useEffect(() => {
        loadProducts()
    }, [loadProducts])

    useEffect(() => {
        let metricsInterval
        if (isRunning) {
            metricsInterval = setInterval(() => {
                const s = statsRef.current
                const sorted = [...s.latencies].sort((a, b) => a - b)
                const pct = (p) => sorted.length === 0 ? 0 : sorted[Math.ceil((p / 100) * sorted.length) - 1]
                const successRate = s.total === 0 ? 100 : Math.round((s.success / s.total) * 100)

                setMetrics(prev => ({
                    ...prev,
                    totalRequests: s.total,
                    successCount: s.success,
                    errorCount: s.errors,
                    currentRps: s.requestsThisSecond,
                    p50: pct(50), p95: pct(95), p99: pct(99),
                    byScenario: JSON.parse(JSON.stringify(s.byScenario)),
                    successRate,
                    rpsHistory: [...(prev.rpsHistory.slice(-29)), s.requestsThisSecond]
                }))

                statsRef.current.requestsThisSecond = 0
                if (s.latencies.length > 2000) statsRef.current.latencies = s.latencies.slice(-2000)
            }, 1000)
        } else {
            setMetrics(prev => ({ ...prev, currentRps: 0 }))
        }
        return () => clearInterval(metricsInterval)
    }, [isRunning])

    const pickScenario = useCallback(() => {
        const conf = configRef.current
        const total = Object.values(conf.scenarios).reduce((a, b) => a + b, 0)
        if (total === 0) return 'happy'
        let r = Math.random() * total
        for (const [id, weight] of Object.entries(conf.scenarios)) {
            r -= weight
            if (r <= 0) return id
        }
        return 'happy'
    }, [])

    const executeRequest = useCallback(async () => {
        const scenario = pickScenario()
        const start = performance.now()
        let ok = false
        try {
            const currentProducts = productsRef.current
            const randomProduct = currentProducts.length > 0 ? currentProducts[Math.floor(Math.random() * currentProducts.length)] : null

            let res
            if (scenario === 'read_orders') {
                res = await fetch('/proxy/order/orders?limit=5')
            } else if (scenario === 'happy') {
                if (!randomProduct) throw new Error('No products')
                res = await fetch('/proxy/order/orders', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
                    body: JSON.stringify({ customerEmail: 'loadtest@zentra.local', items: [{ productId: randomProduct.productId, quantity: 1, price: 999 }] })
                })
            } else if (scenario === 'out_of_stock') {
                if (!randomProduct) throw new Error('No products')
                res = await fetch('/proxy/order/orders', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
                    body: JSON.stringify({ customerEmail: 'loadtest@zentra.local', items: [{ productId: randomProduct.productId, quantity: 99999, price: 999 }] })
                })
            } else if (scenario === 'invalid_product') {
                res = await fetch('/proxy/order/orders', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
                    body: JSON.stringify({ customerEmail: 'loadtest@zentra.local', items: [{ productId: `FAKE-${crypto.randomUUID().substring(0, 8)}`, quantity: 1, price: 999 }] })
                })
            }

            ok = res && (res.ok || res.status === 201 || res.status === 202)
            if (!ok) throw new Error(`HTTP ${res?.status}`)
            await res.text()
            statsRef.current.success++
            statsRef.current.byScenario[scenario].success++
        } catch (e) {
            statsRef.current.errors++
            statsRef.current.byScenario[scenario].error++
        } finally {
            statsRef.current.latencies.push(Math.round(performance.now() - start))
            statsRef.current.total++
            statsRef.current.requestsThisSecond++
        }
    }, [pickScenario])

    const startTest = useCallback(() => {
        if (isRunning) return
        statsRef.current = {
            latencies: [], requestsThisSecond: 0, success: 0, errors: 0, total: 0,
            byScenario: { happy: { success: 0, error: 0 }, out_of_stock: { success: 0, error: 0 }, invalid_product: { success: 0, error: 0 }, read_orders: { success: 0, error: 0 } }
        }
        setMetrics({ totalRequests: 0, successCount: 0, errorCount: 0, currentRps: 0, p50: 0, p95: 0, p99: 0, byScenario: { happy: { success: 0, error: 0 }, out_of_stock: { success: 0, error: 0 }, invalid_product: { success: 0, error: 0 }, read_orders: { success: 0, error: 0 } }, successRate: 100, rpsHistory: [] })
        setIsRunning(true)
        
        const rps = configRef.current.rps
        runnerRef.current = setInterval(executeRequest, Math.max(20, 1000 / rps))
    }, [isRunning, executeRequest])

    const stopTest = useCallback(() => {
        if (!isRunning) return
        clearInterval(runnerRef.current)
        setIsRunning(false)
    }, [isRunning])

    // Re-adjust interval if RPS changes while running
    useEffect(() => {
        if (isRunning) {
            clearInterval(runnerRef.current)
            runnerRef.current = setInterval(executeRequest, Math.max(20, 1000 / config.rps))
        }
    }, [config.rps, isRunning, executeRequest])

    const value = {
        isRunning,
        products,
        isLoadingProducts,
        config,
        setConfig,
        metrics,
        startTest,
        stopTest,
        loadProducts
    }

    return (
        <LoadTestContext.Provider value={value}>
            {children}
        </LoadTestContext.Provider>
    )
}
