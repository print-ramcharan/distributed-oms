# Distributed Order Management System (OMS)

A production-grade, event-driven backend system designed to demonstrate how real-world order processing systems are built, evolve, and handle failures.

This repository follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles and evolves step by step into a **fully distributed, Kafka-based system** with strong boundaries, clear ownership, and production-grade patterns.

---

## Project Goal

This is **not** a UI-focused project.

The goal is to demonstrate:

- How production backend systems are structured
- How event-driven systems evolve safely
- How failures are handled without data corruption
- How clean abstractions enable scalability
- How distributed systems communicate without tight coupling

---

## Current Status

- ✅ **Phase 1 complete**
- ✅ **Phase 2 complete**
- 🚧 **Phase 3 upcoming**

---

# Phase 1: Order Service Foundation

## Phase 1 Objective

Build a clean, stable foundation for the Order Service that can evolve into a distributed system **without rewriting core business logic**.

---

## Order Service Overview

The Order Service is the **entry point** of the system.

Responsibilities:
- Accept customer orders
- Validate input
- Create an Order aggregate
- Persist the order
- Emit a domain event indicating an order was created

---

## Features Implemented (Phase 1)

- REST API to create orders (`POST /orders`)
- Request validation with clean error responses
- Domain-driven design (aggregates, value objects)
- Clean architecture layering:
  - API
  - Application
  - Domain
  - Infrastructure
- Repository abstraction
- Domain event publishing via interface
- Logging-based event publisher (infrastructure implementation)
- Global exception handling with consistent API responses
- No framework leakage into the domain layer

---

## API Example

### Create Order

**Endpoint**
```
POST /orders
```

**Request**
```json
{
  "items": [
    { "productId": "p1", "quantity": 2 }
  ]
}
```

**Success Response (201)**

```json
{
  "orderId": "uuid",
  "status": "PENDING"
}
```

**Validation Error (400)**

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "items": "items must not be empty"
  },
  "timestamp": "..."
}
```

---

## Architecture Intent (Phase 1)

* Controllers handle only HTTP concerns
* Application services orchestrate use cases
* Domain layer contains pure business logic
* Infrastructure adapts external concerns (logging, persistence, messaging)
* Domain events are published via interfaces
* Dependency direction is strictly enforced

```
API → Application → Domain ← Infrastructure
```

The **domain layer has zero Spring dependencies**.

---

## Domain Events (Phase 1)

* `OrderCreatedEvent` emitted whenever an order is created
* Published via a domain-defined `OrderEventPublisher`
* Initially implemented using logging
* Designed to be replaced later by Kafka **without changing business logic**

---

## Tech Stack (Phase 1)

* Java 17
* Spring Boot
* Maven
* REST (JSON)
* In-memory persistence
* Clean Architecture
* Domain-Driven Design

---

# Phase 2: Event-Driven Order → Payment Processing

## Phase 2 Objective

Evolve the system into a **true distributed architecture** by:

* Introducing Kafka-based messaging
* Adding a Payment Service
* Enforcing asynchronous communication
* Preserving clean architecture boundaries
* Solving real-world serialization and contract issues

This phase intentionally focused on **production-grade problems**, not happy-path demos.

---

## Architecture Evolution (Phase 2)

```
Client
  ↓
Order Service
  ├── Validates request
  ├── Creates Order aggregate
  ├── Calculates total amount
  └── Publishes OrderCreatedEvent ───▶ Kafka ───▶ Payment Service
                                               ├── Consumes event
                                               ├── Creates Payment
                                               └── Persists to DB
```

There are **no synchronous calls** between services.

---

## New Service: Payment Service

A fully independent Spring Boot application.

Responsibilities:

* Consume `OrderCreatedEvent`
* Create a payment in `PENDING` state
* Persist payment data
* Remain completely decoupled from Order Service internals

---

## Kafka Integration (Order Service)

### Implemented

* Kafka producer configuration
* Kafka-backed `OrderEventPublisher`
* Topic: `order.created`
* JSON serialization via Spring Kafka

### Key Design Rule

The **domain layer still knows nothing about Kafka**.

Kafka remains an **infrastructure concern only**.

---

## Real Production Issue Encountered

### Error

```
ClassNotFoundException:
com.oms.orderservice.domain.event.OrderCreatedEvent
```

### Root Cause

* Kafka JSON serializer embeds fully qualified class names
* Payment Service does not have Order Service classes
* Separate classloaders per service
* Naive event sharing breaks deserialization

This is a **classic distributed systems failure**.

---

## Correct Solution: Shared Event Contracts

### Design Decision

Introduce a dedicated module:

```
services/event-contracts
```

Purpose:

* Own all cross-service event definitions
* Be framework-agnostic
* Act as the single source of truth for event schemas

---

## Event Contract Design

### `OrderCreatedEvent`

Moved into `event-contracts`.

Characteristics:

* Immutable data carrier
* No business logic
* No Spring dependencies
* Safe for serialization

Contains:

* `orderId`
* `amount`

---

## Domain Enhancements (Order Service)

To support payment creation:

### OrderItem

* Added `price`
* Added `totalPrice()` calculation

### Order Aggregate

* Computes total order amount
* Publishes amount as part of the event

This ensures:

* Payment Service does not re-calculate business logic
* Single source of truth remains the Order Service

---

## Payment Service Implementation

### Kafka Consumer

* Subscribes to `order.created`
* Deserializes shared `OrderCreatedEvent`
* Fully asynchronous

### Payment Creation Logic

* Idempotent (find-or-create)
* Initial status: `PENDING`
* Currency: `INR`
* Independent PostgreSQL database

---

## End-to-End Verification

### Manual Flow

1. Start Kafka & Zookeeper
2. Start Order Service
3. Start Payment Service
4. Call `POST /orders`
5. Observe:

  * Kafka message published
  * Payment Service consumes event
  * Payment persisted successfully

### Database Proof

```sql
SELECT order_id, amount, status FROM payments;
```

Result:

* Correct `order_id`
* Correct `amount`
* Status = `PENDING`

This confirms **true asynchronous consistency**.

---

## Codebase Cleanup & Discipline

* Removed duplicated event classes
* Centralized shared contracts
* Clean commit history
* No temporary hacks
* IDE files ignored
* Each service independently buildable

---

## Architectural Principles Reinforced

* Event-driven architecture
* Loose coupling between services
* Contract-first messaging
* No framework leakage into domain
* No synchronous service dependencies
* Clear ownership of business logic
* Infrastructure replaceable without domain changes

---

## Phase 2 Summary

Phase 2 introduced Kafka-based asynchronous communication between Order and Payment services using shared event contracts, enabling safe serialization, clean service decoupling, and end-to-end order-to-payment processing without synchronous dependencies.

---

## Repository Structure

```
distributed-oms/
├── pom.xml
└── services/
    ├── order-service/
    ├── payment-service/
    └── event-contracts/
```

Each service is an independent Spring Boot application.

---

## Roadmap

* **Phase 3:** Saga orchestration & distributed transactions
* **Phase 4:** Reliability (retries, DLQ, observability)
* **Phase 5:** Scalability & performance testing
* **Phase 6:** Production readiness (security, deployment, chaos testing)

---

## Status

🚧 Actively evolving
📌 Phase 1 & Phase 2 complete
