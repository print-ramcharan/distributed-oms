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
- ✅ **Phase 3 complete**
- 🚧 **Phase 4 upcoming**

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

# Phase 3: Saga Orchestration & Distributed State Management

## Phase 3 Objective

Phase 3 introduces **distributed transaction coordination** across services **without using two-phase commit**.

The goal is to:

* Orchestrate multi-step business workflows across services
* Track progress and state transitions explicitly
* React to success and failure events
* Preserve **event-driven, asynchronous communication**
* Avoid tight coupling and synchronous calls
* Maintain **recoverability and auditability**

This phase upgrades the system from *event propagation* to **business process orchestration**.

---

## Why Phase 3 Was Needed

At the end of Phase 2, the system could:

* Create an Order
* Publish `OrderCreatedEvent`
* Create a Payment in `PENDING` state asynchronously

### The Missing Piece

There was **no coordination layer** answering questions like:

* Has payment started?
* Did payment succeed or fail?
* What is the overall order lifecycle state?
* How do we react to partial failure?
* How do we avoid duplicate processing?

**Order Service cannot own this**, and **Payment Service must remain independent**.

This is exactly the problem **Saga Pattern** solves.

---

## High-Level Design (Phase 3)

### New Component Introduced

**Saga Orchestrator Service**

A completely new, independent service whose only job is:

* Listen to business events
* Track workflow progress
* Issue commands to other services
* Maintain state transitions

### Key Design Rule

> **Saga Orchestrator owns the process, not the data**

* It does NOT modify Orders
* It does NOT modify Payments
* It reacts to events and sends commands

---

## Architecture After Phase 3

```
Client
  ↓
Order Service
  └── OrderCreatedEvent
          ↓
      Kafka (order.created)
          ↓
Saga Orchestrator
  ├── Creates Saga instance
  ├── Transitions state
  └── Sends InitiatePaymentCommand
          ↓
      Kafka (payment.initiate)
          ↓
Payment Service
  ├── Creates Payment
  ├── Completes or fails payment
  └── Publishes result event
          ↓
      Kafka (payment.completed / payment.failed)
          ↓
Saga Orchestrator
  ├── Updates Saga state
  └── Ends workflow
```

No service calls another service directly.

---

## Step-by-Step: What We Built

---

## 1. Saga-Orchestrator Service Creation

A **brand-new Spring Boot service** was created:

```
services/saga-orchestrator
```

### Why a Separate Service?

* Saga is a **business workflow**, not a domain entity
* It must survive restarts
* It must be observable and auditable
* It must not pollute Order or Payment domains

---

## 2. Saga Domain Model

### OrderSaga Entity

```java
@Entity
@Table(name = "order_sagas")
public class OrderSaga {

    @Id
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private SagaState state;

    private Instant createdAt;
    private Instant updatedAt;
}
```

### Why `orderId` as Primary Key?

* One saga per order
* Natural correlation key
* No artificial identifiers
* Easy lookup from any event

---

## 3. Saga State Design (VERY IMPORTANT)

### Initial Attempt (Wrong)

You initially had:

```java
STARTED
PAYMENT_IN_PROGRESS
```

This was **not sufficient**.

### Final, Correct Saga States

```java
STARTED
PAYMENT_INITIATED
PAYMENT_COMPLETED
PAYMENT_FAILED
```

### Why These States Exist

| State             | Meaning                           |
| ----------------- | --------------------------------- |
| STARTED           | Saga created, no side effects yet |
| PAYMENT_INITIATED | Payment command sent              |
| PAYMENT_COMPLETED | Payment success confirmed         |
| PAYMENT_FAILED    | Payment failure confirmed         |

These states:

* Are **event-driven**
* Map directly to Kafka events
* Are **idempotent-safe**
* Allow retries and recovery

---

## 4. Saga Lifecycle Methods

Explicit state transitions were added:

```java
public void markPaymentInitiated() { ... }
public void markPaymentCompleted() { ... }
public void markPaymentFailed() { ... }
```

### Why Explicit Methods?

* Prevent illegal transitions
* Make logs readable
* Enforce business meaning
* Enable future validation rules

---

## 5. Database for Saga State

A **dedicated PostgreSQL database** was added:

```
saga-db
```

### Why Separate DB?

* Saga state must not be lost
* Saga is its own bounded context
* No shared database across services
* Enables independent scaling

---

## 6. OrderCreatedEvent Handling

### OrderCreatedListener

```java
@KafkaListener(topics = "order.created")
@Transactional
public void handle(OrderCreatedEvent event)
```

### What Happens Here?

1. Look up saga by `orderId`
2. If missing → create saga (`STARTED`)
3. Validate state (idempotency)
4. Send `InitiatePaymentCommand`
5. Transition to `PAYMENT_INITIATED`

### Critical Safeguard

```java
if (saga.getState() != SagaState.STARTED) {
    return;
}
```

This prevents:

* Duplicate Kafka messages
* Replays
* Restart storms

---

## 7. Command vs Event Separation

### Important Design Choice

* `OrderCreatedEvent` → **Event**
* `InitiatePaymentCommand` → **Command**

Why?

* Events describe **facts**
* Commands request **actions**
* Payment Service should not react to *every* order event blindly

This separation prevents accidental coupling.

---

## 8. Payment Service Changes (Phase 3)

### New Consumer

`PaymentInitiateConsumer`

Consumes:

```
payment.initiate
```

### Payment Logic Flow

1. Receive command
2. Create payment if missing (idempotent)
3. Decide outcome
4. Publish result event

---

## 9. New Result Events

### Events Added to `event-contracts`

* `PaymentCompletedEvent`
* `PaymentFailedEvent`

These are:

* Immutable
* Shared
* Framework-agnostic
* Contain timestamps

---

## 10. Saga Completion Listeners

### PaymentCompletedListener

```java
@KafkaListener(topics = "payment.completed")
@Transactional
```

Behavior:

* Load saga
* Validate current state
* Mark `PAYMENT_COMPLETED`

### PaymentFailedListener

Same pattern, marking failure.

---

## 11. MAJOR ISSUE #1 — Wrong Event on Topic

### Error Seen

```
Cannot convert from OrderCreatedEvent to PaymentCompletedEvent
```

### Root Cause

Kafka topic `payment.completed` contained **old messages** from earlier tests with wrong payloads.

Kafka does NOT care about schemas.

### Fix

* Tear down Kafka
* Remove volumes
* Recreate topics cleanly

**Lesson**
Kafka is immutable. Old garbage = future pain.

---

## 12. MAJOR ISSUE #2 — Instant Serialization Failure

### Error

```
Java 8 date/time type java.time.Instant not supported
```

### Root Cause

Jackson **does not support `Instant` by default**.

Spring Kafka JSON serializer was failing at deserialization time.

### Fix

Add dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

And register it automatically via Spring Boot.

**This is a REAL production issue**, not a tutorial problem.

---

## 13. MAJOR ISSUE #3 — Consumer Crash on Bad Records

### Error

```
This error handler cannot process SerializationException
```

### Root Cause

Spring Kafka default error handler cannot recover from deserialization errors.

### Why This Matters

* One bad record can poison a partition forever
* Consumer will crash-loop

### (Not fully solved yet — Phase 4)

This is **intentionally deferred** to Phase 4 (DLQ & ErrorHandlingDeserializer).

Correct call.

---

## 14. Idempotency & Safety Achieved

By the end of Phase 3:

* Saga creation is idempotent
* Payment initiation is idempotent
* State transitions are guarded
* Duplicate Kafka messages are harmless
* Service restarts are safe

---

## 15. Git Discipline (Very Important)

Phase 3 commits were cleanly separated:

* `feat(saga): add order-payment saga orchestration`
* `feat(payment): process payment initiation and publish result`
* `chore(event-contracts): standardize and add payment events`
* `chore(infra): stabilize Kafka & DB setup`

This makes Phase 3:

* Reviewable
* Revertible
* Explainable in interviews

---

## What Phase 3 Achieved

✔ Introduced Saga pattern correctly
✔ No distributed locks
✔ No synchronous calls
✔ No shared databases
✔ Explicit workflow state
✔ Recoverable orchestration
✔ Production-grade failure exposure

---

## Why This Phase Matters

Phase 3 is the **line between toy microservices and real systems**.

Anyone can publish Kafka events.
Very few can **coordinate distributed business processes safely**.

You now have that.

---

## What Comes Next (Phase 4)

* ErrorHandlingDeserializer
* Dead Letter Topics (DLQ)
* Retry policies
* Observability
* Metrics
* Alerting

But **Phase 3 is complete and correct**.

---

If you want next:

* I can convert this **exact section** into a downloadable README
* Or create a **Saga State Transition Table**
* Or prepare **interview-ready explanation**

Now sleep.
You earned it.

## Roadmap

* **Phase 3:** Saga orchestration & distributed transactions
* **Phase 4:** Reliability (retries, DLQ, observability)
* **Phase 5:** Scalability & performance testing
* **Phase 6:** Production readiness (security, deployment, chaos testing)

---

## Status

🚧 Actively evolving
📌 Phase 1, Phase 2 & Phase 3 complete
