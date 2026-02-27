import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '10s', target: 0 },
    ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
    const res = http.get(`${BASE_URL}/orders?limit=500`, { timeout: '5s' });

    check(res, { 'fetched orders': r => r.status === 200 });

    return res.json().map(o => o.orderId);
}

export default function (orderIds) {
    const id = orderIds[Math.floor(Math.random() * orderIds.length)];

    const res = http.get(`${BASE_URL}/orders/${id}`, { timeout: '5s' });

    check(res, { 'status is 200': r => r.status === 200 });

    sleep(0.1);
}
