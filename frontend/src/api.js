/**
 * Zentra Control Center — Real Backend API Client
 * All requests go through the Vite dev proxy at /proxy/{service}
 * which routes to the actual Spring Boot services without CORS issues.
 *
 * Service → Port mapping:
 *   order      → 8081
 *   payment    → 8082
 *   inventory  → 8083
 *   notification → 8084
 *   saga       → 8085
 *   fulfillment → 8086
 *   gateway    → 8080
 *   prometheus → 9090
 */

const SVC = {
    order: '/proxy/order',
    payment: '/proxy/payment',
    inventory: '/proxy/inventory',
    notification: '/proxy/notification',
    saga: '/proxy/saga',
    fulfillment: '/proxy/fulfillment',
    gateway: '/proxy/gateway',
    prometheus: '/proxy/prometheus',
}

async function api(url, options = {}) {
    try {
        const res = await fetch(url, {
            headers: { 'Content-Type': 'application/json', ...options.headers },
            ...options,
        })
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
        const text = await res.text()
        return text ? JSON.parse(text) : {}
    } catch (err) {
        console.warn(`[API] ${url} failed:`, err.message)
        return null
    }
}

// =============================================
//  SERVICE HEALTH
// =============================================
export const SERVICE_DEFS = [
    { id: 'order', name: 'Order Service', port: 8081, svc: SVC.order },
    { id: 'payment', name: 'Payment Service', port: 8082, svc: SVC.payment },
    { id: 'inventory', name: 'Inventory Service', port: 8083, svc: SVC.inventory },
    { id: 'notification', name: 'Notification Service', port: 8084, svc: SVC.notification },
    { id: 'saga', name: 'Saga Orchestrator', port: 8085, svc: SVC.saga },
    { id: 'fulfillment', name: 'Fulfillment Service', port: 8086, svc: SVC.fulfillment },
    { id: 'gateway', name: 'API Gateway', port: 8080, svc: SVC.gateway },
]

export async function fetchServiceHealth(svcDef) {
    const data = await api(`${svcDef.svc}/actuator/health`)
    if (!data) return { id: svcDef.id, name: svcDef.name, port: svcDef.port, status: 'DOWN', components: {} }
    return {
        id: svcDef.id,
        name: svcDef.name,
        port: svcDef.port,
        status: data.status === 'UP' ? 'UP' : 'DOWN',
        components: data.components || {},
    }
}

export async function fetchAllServiceHealth() {
    return Promise.all(SERVICE_DEFS.map(fetchServiceHealth))
}

// =============================================
//  JVM METRICS (from Spring Actuator)
// =============================================
export async function fetchMetric(svcDef, metricName) {
    const data = await api(`${svcDef.svc}/actuator/metrics/${metricName}`)
    if (!data || !data.measurements) return null
    return data.measurements[0]?.value ?? null
}

export async function fetchServiceMetrics(svcDef) {
    const [memUsed, memMax, cpuUsage, gcPause, threads, httpRequests] = await Promise.all([
        fetchMetric(svcDef, 'jvm.memory.used'),
        fetchMetric(svcDef, 'jvm.memory.max'),
        fetchMetric(svcDef, 'process.cpu.usage'),
        fetchMetric(svcDef, 'jvm.gc.pause'),
        fetchMetric(svcDef, 'jvm.threads.live'),
        fetchMetric(svcDef, 'http.server.requests'),
    ])
    return {
        id: svcDef.id,
        name: svcDef.name,
        memUsedMb: memUsed != null ? Math.round(memUsed / 1024 / 1024) : null,
        memMaxMb: memMax != null ? Math.round(memMax / 1024 / 1024) : null,
        cpuPercent: cpuUsage != null ? +(cpuUsage * 100).toFixed(1) : null,
        gcPauseMs: gcPause != null ? +(gcPause * 1000).toFixed(1) : null,
        threads: threads != null ? Math.round(threads) : null,
        httpRequests: httpRequests != null ? Math.round(httpRequests) : null,
    }
}

export async function fetchAllServiceMetrics() {
    // Only poll services that are likely UP (order + saga are main ones)
    return Promise.all(SERVICE_DEFS.slice(0, 6).map(fetchServiceMetrics))
}

// =============================================
//  PROMETHEUS QUERIES
// =============================================
export async function queryPrometheus(expr) {
    const url = `${SVC.prometheus}/api/v1/query?query=${encodeURIComponent(expr)}`
    const data = await api(url)
    return data?.data?.result ?? []
}

export async function fetchPrometheusMetrics() {
    const [httpRate, errorRate, p99Latency, kafkaLag] = await Promise.all([
        queryPrometheus('sum(rate(http_server_requests_seconds_count{job=~"order-service|payment-service"}[1m]))'),
        queryPrometheus('sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))'),
        queryPrometheus('histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))'),
        queryPrometheus('sum(kafka_consumer_records_lag) by (topic)'),
    ])

    const getValue = (result) => result[0]?.value?.[1] ? parseFloat(result[0].value[1]) : null

    return {
        httpRate: getValue(httpRate),
        errorRate: getValue(errorRate),
        p99LatencyMs: p99Latency[0]?.value?.[1] ? +(parseFloat(p99Latency[0].value[1]) * 1000).toFixed(0) : null,
        kafkaLag: kafkaLag.reduce((sum, r) => sum + parseFloat(r.value[1] || '0'), 0),
    }
}

// =============================================
//  ORDERS
// =============================================
export async function fetchOrders(limit = 50) {
    return await api(`${SVC.order}/orders?limit=${limit}`) ?? []
}

export async function createOrder(customerEmail, items) {
    const idempotencyKey = crypto.randomUUID()
    return await api(`${SVC.order}/orders`, {
        method: 'POST',
        headers: {
            'Idempotency-Key': idempotencyKey,
            'X-User-Id': crypto.randomUUID(),
        },
        body: JSON.stringify({ customerEmail, items }),
    })
}

export async function cancelOrder(orderId) {
    return await api(`${SVC.order}/orders/${orderId}/cancel`, { method: 'PUT' })
}

export async function fetchOrderById(orderId) {
    return await api(`${SVC.order}/orders/${orderId}`)
}

// =============================================
//  SAGA
// =============================================
export async function fetchSaga(orderId) {
    return await api(`${SVC.saga}/sagas/${orderId}`)
}

// =============================================
//  INVENTORY
// =============================================
export async function fetchInventory() {
    return await api(`${SVC.inventory}/inventory`) ?? []
}

export async function restockProduct(productId, quantity) {
    return await api(`${SVC.inventory}/inventory/${productId}/add?quantity=${quantity}`, { method: 'POST' })
}

// =============================================
//  PAYMENTS
// =============================================
export async function fetchPaymentForOrder(orderId) {
    return await api(`${SVC.payment}/payments/order/${orderId}`)
}

export async function refundPayment(orderId) {
    return await api(`${SVC.payment}/payments/refund/${orderId}`, { method: 'POST' })
}

// =============================================
//  DLQ
// =============================================
export async function fetchDlqMessages(status) {
    const url = status
        ? `${SVC.order}/admin/dlq?status=${status}`
        : `${SVC.order}/admin/dlq`
    return await api(url) ?? []
}

export async function retryDlqMessage(id) {
    return await api(`${SVC.order}/admin/dlq/${id}/retry`, { method: 'POST' })
}

export async function resolveDlqMessage(id) {
    return await api(`${SVC.order}/admin/dlq/${id}/resolve`, { method: 'POST' })
}

// =============================================
//  CHAOS
// =============================================
export async function fetchChaosStatus() {
    return await api(`${SVC.order}/chaos/status`) ?? {}
}

export async function toggleKillSwitch(serviceId, enable) {
    // Each service has its own /chaos/kill-service endpoint
    const svcDef = SERVICE_DEFS.find(s => s.id === serviceId)
    if (!svcDef) return null
    return await api(`${svcDef.svc}/chaos/kill-service?enable=${enable}`, { method: 'POST' })
}

export async function toggleLatency(serviceId, enable) {
    const svcDef = SERVICE_DEFS.find(s => s.id === serviceId)
    if (!svcDef) return null
    return await api(`${svcDef.svc}/chaos/latency?enable=${enable}`, { method: 'POST' })
}

export async function setThrottle(tps) {
    return await api(`${SVC.order}/chaos/throttle?tps=${tps}`, { method: 'POST' })
}

// =============================================
//  ACTUATOR — Loggers
// =============================================
export async function fetchLoggers(svcDef) {
    return await api(`${svcDef.svc}/actuator/loggers`)
}

export async function setLogLevel(svcDef, loggerName, level) {
    return await api(`${svcDef.svc}/actuator/loggers/${loggerName}`, {
        method: 'POST',
        body: JSON.stringify({ configuredLevel: level }),
    })
}

// =============================================
//  ACTUATOR — Environment
// =============================================
export async function fetchEnv(svcDef) {
    return await api(`${svcDef.svc}/actuator/env`)
}

// =============================================
//  NOTIFICATIONS
// =============================================
export async function fetchNotifications() {
    return await api(`${SVC.notification}/notifications`) ?? []
}

// =============================================
//  FULFILLMENT
// =============================================
export async function fetchFulfillments() {
    return await api(`${SVC.fulfillment}/fulfillment`) ?? []
}
