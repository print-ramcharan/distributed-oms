import { create } from 'zustand'

// =============================================
// ORDER SAGA STATE
// =============================================
export const useSagaStore = create((set, get) => ({
    orders: [],
    activeOrder: null,
    sagaSteps: {},

    createOrder: (product) => {
        const orderId = `ORD-${Math.random().toString(36).substr(2, 8).toUpperCase()}`;
        const order = {
            id: orderId,
            product: product.name,
            amount: product.price,
            status: 'PENDING',
            createdAt: new Date(),
            steps: [
                { id: 'order_created', label: 'Order Created', status: 'processing', icon: 'ShoppingCart', startedAt: new Date() },
                { id: 'payment', label: 'Payment Service', status: 'pending', icon: 'CreditCard' },
                { id: 'inventory', label: 'Inventory Service', status: 'pending', icon: 'Package' },
                { id: 'fulfillment', label: 'Fulfillment Service', status: 'pending', icon: 'Truck' },
                { id: 'notification', label: 'Notification Service', status: 'pending', icon: 'Bell' },
            ],
        };
        set((state) => ({ orders: [order, ...state.orders], activeOrder: order }));
        get().simulateSaga(orderId);
        return orderId;
    },

    simulateSaga: (orderId, scenario = 'success') => {
        const delays = scenario === 'payment_failure'
            ? [1200, 2500]
            : scenario === 'inventory_failure'
                ? [1200, 2500, 3800]
                : [1200, 2500, 3800, 5000, 6000];

        const stepOrder = ['payment', 'inventory', 'fulfillment', 'notification'];

        stepOrder.forEach((stepId, idx) => {
            setTimeout(() => {
                const shouldFail = (scenario === 'payment_failure' && stepId === 'payment') ||
                    (scenario === 'inventory_failure' && stepId === 'inventory');
                set((state) => ({
                    orders: state.orders.map(o =>
                        o.id === orderId ? {
                            ...o,
                            status: shouldFail ? 'COMPENSATING' : (idx === stepOrder.length - 1 ? 'CONFIRMED' : 'PROCESSING'),
                            steps: o.steps.map((s, sIdx) => {
                                if (s.id === stepId) return { ...s, status: shouldFail ? 'failed' : 'completed', completedAt: new Date() };
                                // If failure, compensate previous steps
                                if (shouldFail && sIdx < stepOrder.indexOf(stepId) + 1 && s.id !== 'order_created') {
                                    return { ...s, status: 'compensating' };
                                }
                                // Set next step to processing
                                if (!shouldFail && s.id === stepOrder[idx + 1]) return { ...s, status: 'processing', startedAt: new Date() };
                                return s;
                            }),
                        } : o
                    ),
                    activeOrder: state.activeOrder?.id === orderId
                        ? {
                            ...state.activeOrder,
                            status: shouldFail ? 'COMPENSATING' : (idx === stepOrder.length - 1 ? 'CONFIRMED' : 'PROCESSING'),
                        }
                        : state.activeOrder,
                }));

                if (shouldFail) {
                    // After compensation, mark order as cancelled
                    setTimeout(() => {
                        set((state) => ({
                            orders: state.orders.map(o =>
                                o.id === orderId ? { ...o, status: 'CANCELLED' } : o
                            ),
                        }));
                    }, 2000);
                }
            }, delays[idx]);
        });
    },

    setScenario: (orderId, scenario) => {
        set((state) => ({ activeOrder: state.orders.find(o => o.id === orderId) || state.activeOrder }));
    },

    clearOrders: () => set({ orders: [], activeOrder: null }),

    // =============================================
    // SERVICE HEALTH STATE
    // =============================================
    services: [
        { id: 'order-service', name: 'Order Service', status: 'UP', port: 8081, latency: 45 },
        { id: 'payment-service', name: 'Payment Service', status: 'UP', port: 8082, latency: 120 },
        { id: 'inventory-service', name: 'Inventory Service', status: 'UP', port: 8083, latency: 67 },
        { id: 'fulfillment-service', name: 'Fulfillment Service', status: 'UP', port: 8084, latency: 89 },
        { id: 'notification-service', name: 'Notification Service', status: 'UP', port: 8085, latency: 34 },
        { id: 'saga-orchestrator', name: 'Saga Orchestrator', status: 'UP', port: 8086, latency: 22 },
    ],

    toggleService: (serviceId) => {
        set((state) => ({
            services: state.services.map(s =>
                s.id === serviceId
                    ? { ...s, status: s.status === 'UP' ? 'DOWN' : 'UP', circuitBreaker: s.status === 'UP' ? 'OPEN' : 'CLOSED' }
                    : s
            ),
        }));
    },

    // =============================================
    // DLQ STATE
    // =============================================
    dlqMessages: [
        { id: 'dlq-001', topic: 'payment-events', errorMsg: 'Payment gateway timeout after 3 retries', payload: '{"orderId":"ORD-A1B2C3","amount":5000}', retries: 3, timestamp: new Date(Date.now() - 600000) },
        { id: 'dlq-002', topic: 'inventory-events', errorMsg: 'Product not found in warehouse', payload: '{"orderId":"ORD-D4E5F6","productId":"PROD-XYZ"}', retries: 3, timestamp: new Date(Date.now() - 1800000) },
    ],

    replayDlqMessage: (id) => {
        set((state) => ({
            dlqMessages: state.dlqMessages.filter(m => m.id !== id),
        }));
    },
}));

// =============================================
// LIVE METRICS STORE
// =============================================
export const useMetricsStore = create((set) => ({
    ordersPerMin: 42,
    successRate: 99.7,
    avgLatency: 124,
    kafkaLag: 0,
    activeTransactions: 7,
    errorRate: 0.3,

    tick: () => set((state) => ({
        ordersPerMin: Math.max(30, Math.min(80, state.ordersPerMin + (Math.random() - 0.5) * 6)),
        successRate: Math.max(98, Math.min(100, state.successRate + (Math.random() - 0.5) * 0.2)),
        avgLatency: Math.max(80, Math.min(300, state.avgLatency + (Math.random() - 0.5) * 20)),
        kafkaLag: Math.max(0, Math.min(50, state.kafkaLag + Math.floor((Math.random() - 0.5) * 4))),
        activeTransactions: Math.max(0, Math.min(20, state.activeTransactions + Math.floor((Math.random() - 0.5) * 3))),
        errorRate: Math.max(0, Math.min(2, state.errorRate + (Math.random() - 0.5) * 0.1)),
    })),
}));
