import { create } from 'zustand'

// =============================================
// ORDER SAGA STATE
// =============================================
export const useSagaStore = create((set, get) => ({
    // Store only the active order ID for the simulator to track
    activeOrderId: null,
    setActiveOrderId: (id) => set({ activeOrderId: id }),
    clearActiveOrder: () => set({ activeOrderId: null }),
}));

// =============================================
// LIVE METRICS STORE
// =============================================
export const useMetricsStore = create((set) => ({
    ordersPerMin: 0,
    successRate: 100,
    avgLatency: 0,
    kafkaLag: 0,
    activeTransactions: 0,
    errorRate: 0,

    setMetrics: (metrics) => set({ ...metrics }),
}));

