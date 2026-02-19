# Distributed OMS - API Reference (V1.1)

This document provides a reference for all available REST endpoints in the Distributed Order Management System. All endpoints (except Auth) are accessible via the Gateway and require a valid JWT token.

## Base URL
`http://localhost:8080/api` (via Gateway)

---

### 🛒 Order Service
Manage orders and customer history.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/orders` | `POST` | Create a new order (requires Idempotency-Key). |
| `/orders/{id}` | `GET` | Get order details by ID. |
| `/orders/customer/{email}` | `GET` | **[NEW]** List all orders for a specific customer. |
| `/orders/{id}/cancel` | `PUT` | **[NEW]** Cancel an order (triggers compensation). |

---

### 📦 Inventory Service
Monitor and update stock levels.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/inventory` | `GET` | **[NEW]** List all products and current stock. |
| `/inventory/{id}` | `GET` | Get stock level for a specific product. |
| `/inventory/{id}/add` | `POST` | **[NEW]** Restock a product. Params: `quantity`. |

---

### 💳 Payment Service
Track payments and process refunds.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/payments/order/{orderId}` | `GET` | **[NEW]** Check payment status for an order. |
| `/payments/refund/{orderId}` | `POST` | **[NEW]** Trigger a manual refund for an order. |

---

### 🚚 Fulfillment Service
Track the shipping lifecycle.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/fulfillment/order/{orderId}` | `GET` | **[NEW]** Get the current fulfillment/shipping status. |
| `/fulfillment` | `GET` | **[NEW]** List all active fulfillment tasks. |

---

### 🔄 Saga Orchestrator
Monitor distributed transaction states.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/sagas/{orderId}` | `GET` | **[NEW]** View the internal state of the Order Saga. |

---

### 🔔 Notification Service
View notification history.

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/notifications` | `GET` | **[NEW]** List all sent notifications (emails/logs). |

---

### 🛡️ Authentication
| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/auth/login` | `POST` | Generate a JWT token. Params: `userId`. |
