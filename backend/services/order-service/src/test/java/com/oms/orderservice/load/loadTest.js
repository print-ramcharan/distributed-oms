import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '10s', target: 0 },
  ],
};
export default function () {
  const payload = JSON.stringify({
    items: [
      { productId: "prod-101", quantity: 2, price: 50.00 },
      { productId: "prod-202", quantity: 1, price: 100.50 }
    ]
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': `key-${Date.now()}-${__VU}-${__ITER}`
    },
  };

  const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
  const res = http.post(`${BASE_URL}/orders`, payload, params);
  check(res, { 'status was 200 or 201': (r) => r.status === 200 || r.status === 201 });
  sleep(1);
}