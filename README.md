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
- ✅ **Phase 4 complete**
-  **Phase 5 upcoming**

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


Now sleep.
You earned it.


Got it 👍
Here is a **clean, non-documentation, straight-to-the-point explanation** of **Phase 4**, exactly at the level you’re asking for.

---

### What Phase 4 Actually Did (In Simple Terms)

Phase 4 was about **making sure one bad Kafka message cannot break the system**.

That’s it.

---

### Problems We Solved

* A malformed message (wrong UUID, bad JSON) **used to crash or loop the consumer**
* Kafka would keep retrying the same offset forever
* No visibility into *what* failed or *why*

---

### What We Added

1. **Safe deserialization**

  * Kafka no longer crashes on bad payloads
  * Deserialization errors are captured, not fatal

2. **Limited retries**

  * Message is tried a few times
  * This covers temporary issues (not permanent ones)

3. **Dead Letter Queue (DLQ)**

  * After retries, bad messages go to `payment.initiate.dlq`
  * Consumer moves forward
  * System stays alive

4. **Failure isolation**

  * One bad message does not block:

    * the partition
    * the consumer group
    * future messages

---

### What You Verified Yourself (Important)

* ❌ Invalid UUID → goes to DLQ
* ✅ Valid UUID → processed normally
* Consumer **did not crash**
* Offsets **continued**
* Service stayed **stable**

This confirms Phase 4 is working correctly.

---

### What We Intentionally Did NOT Do

* No retry.1 / retry.2 topics
* No complex backoff pipelines

Reason:
You already retry a few times **and then DLQ** — that’s enough for now and **perfectly acceptable in production**.

---

### Final Verdict

— **Phase 4 is complete**.

You now have:

* Failure containment
* Consumer stability
* Observability via DLQ
* Production-safe behavior

Nothing critical is missing for Phase 4.

---

Absolutely.
Below is a **clean, complete, documentation-grade version of Phase 4**, rewritten for **long-term reference, interviews, and project docs**.
Nothing is removed. Nothing is assumed. Nothing is missing.

You can paste this directly into a `PHASE_4_RELIABILITY.md`.

---

# Phase 4.5 — Reliability & Failure Isolation

**Transactional Outbox · Retry Control · DLQ · Kafka Fault Tolerance**

---

## 1. Phase 4 Objective

After Phase 3, the system could orchestrate distributed workflows using events, but it was **not safe under real-world failures**.

Phase 4 existed to ensure the system:

* Does **not lose events** when Kafka is unavailable
* Does **not crash** or block core business flows
* Does **not enter retry storms**
* Can **restart safely** without duplication
* Can **isolate poison events**
* Remains **eventually consistent** under all conditions

This phase is about **operational correctness**, not new features.

---

## 2. System State Before Phase 4

### Event publishing model

* Order Service:

    * Persisted order
    * Published `OrderCreatedEvent` directly to Kafka

### Problems with this approach

* If Kafka was down:

    * Order succeeded
    * Event was **lost permanently**
* If Kafka publish failed:

    * Database transaction had already committed
* No retries
* No DLQ
* No audit trail
* No restart safety

This violated core backend guarantees:

* Atomicity
* Reliability
* Recoverability

---

## 3. Step 1 — Transactional Outbox Pattern

### Why it was needed

To guarantee:

> **If the database transaction commits, the event is never lost.**

Kafka availability must not affect business correctness.

---

### What was introduced

A durable `outbox_events` table:

```sql
outbox_events (
  id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  aggregate_type VARCHAR NOT NULL,
  event_type VARCHAR NOT NULL,
  payload JSONB NOT NULL,
  status VARCHAR NOT NULL,
  created_at TIMESTAMP NOT NULL,
  retry_count INTEGER NOT NULL,
  next_retry_at TIMESTAMP
)
```

---

### Core principle

* Order data and outbox row are written **in the same DB transaction**
* Kafka is **never called inside the request thread**
* Event publication becomes **asynchronous and recoverable**

---

### Result

* Orders are **independent of Kafka**
* Events are **durable**
* System becomes restart-safe

---

## 4. Step 2 — Outbox Relay (Asynchronous Publisher)

### What was built

A scheduled relay:

```java
@Scheduled(fixedDelay = 500)
@Transactional
public void publishPendingEvents()
```

---

### Responsibilities

* Poll pending outbox rows
* Deserialize payload
* Publish event to Kafka
* Update outbox state

---

### Query used

```sql
SELECT *
FROM outbox_events
WHERE status = 'NEW'
AND retry_count < 3
AND (next_retry_at IS NULL OR next_retry_at <= now())
ORDER BY created_at
LIMIT 50
FOR UPDATE SKIP LOCKED
```

---

### Why this query matters

* `FOR UPDATE SKIP LOCKED`

    * Prevents duplicate publishing
    * Enables safe parallelism
* Ordering ensures fairness
* Restart-safe by design

---

## 5. First Major Failure — Kafka Caused Infinite Retries

### What happened

When Kafka was down:

* Scheduler ran every 500 ms
* Same event retried continuously
* Logs exploded
* CPU wasted
* System entered a **retry storm**

---

### Why this was dangerous

* Kafka outages are normal in production
* Infinite retries self-DOS the service
* Reliability systems must **degrade gracefully**

---

## 6. Step 3 — Retry Control & Explicit State

### Insight

Not all failures should be treated the same.

---

### Outbox lifecycle

* `NEW` → eligible for publishing
* `SENT` → published successfully
* `FAILED` → permanently dead

Retries must be **bounded**.

---

## 7. Step 4 — Failure Classification (Key Design Decision)

Failures were split into two categories.

---

### 7.1 Transient Failures (Retryable)

Examples:

* Kafka broker down
* Network timeout
* Topic temporarily unavailable

**Behavior:**

* Increment `retry_count`
* Schedule `next_retry_at`
* Keep status as `NEW`
* Retry later

---

### 7.2 Poison / Permanent Failures

Examples:

* JSON deserialization failure
* Corrupt payload
* Domain invariant violation

**Behavior:**

* Send event to DLQ
* Mark event as `FAILED`
* Never retry again

---

### Why this matters

> Retry storms only happen when poison events are retried forever.

Classification is the **core reliability control**.

---

## 8. Step 5 — Dead Letter Queue (DLQ)

### Why DLQ exists

Poison events:

* Will never succeed
* Must not block the pipeline
* Must be manually inspected

---

### DLQ payload contents

Each DLQ message contains:

* Source service
* Original topic
* Event type
* Aggregate ID
* Full payload
* Exception message
* Timestamp

---

### DLQ rules

* Only poison events go to DLQ
* Infra failures never do
* Prevents DLQ spam

---

## 9. Step 6 — Kafka Down Scenario (Intentional Test)

### Test performed

* Order Service running
* Kafka container stopped
* Orders created

---

### Observed behavior

* Orders saved successfully
* Outbox rows created
* Relay attempted publishing
* Kafka timed out
* Events remained `NEW`
* No crashes
* No data loss

This validated:

* Outbox correctness
* Failure isolation
* Business continuity

---

## 10. Step 7 — Service Restart Safety

### Scenario tested

* Kafka down
* Pending outbox events exist
* Order Service restarted

---

### Result

* Relay resumed polling
* Same events retried
* No duplication
* No corruption

Validated:

* Row locking
* Idempotent publishing
* Restart safety

---

## 11. Issues Encountered & Fixes

### Issue 1 — “Topic not present in metadata”

**Cause:** Kafka intentionally down
**Fix:** Classified as transient failure
**Result:** No state corruption

---

### Issue 2 — Infinite retries

**Cause:** No retry boundary
**Fix:** Retry count + DLQ transition

---

### Issue 3 — “Why is it still running?”

**Cause:** Misunderstanding scheduler behavior
**Clarification:**

> Schedulers never stop.
> Work stops when no rows match.

---

### Issue 4 — Tight coupling of relay and Kafka

**Fix:**

* Relay handles orchestration
* Publisher only talks to Kafka

---

## 12. Final System Behavior

### Kafka DOWN

* Orders succeed
* Events stored
* No retry storm
* No crashes
* No data loss

### Kafka UP

* Relay resumes
* Pending events published
* Status becomes `SENT`

### Poison event

* Sent to DLQ
* Marked `FAILED`
* Pipeline continues

### Service restart

* Safe recovery
* No duplication
* No missed events

---

## 13. What Phase 4 Achieved

> **The system is now failure-tolerant, restart-safe, and production-grade.**

---

## 14. Interview-Grade Engineering Demonstrated

* Transactional Outbox pattern
* Failure classification
* DLQ isolation
* Retry storm prevention
* Idempotent publishing
* Operational resilience

This is **real backend engineering**, not CRUD.

---

## 15. Phase 4 Status

✅ **COMPLETE**

No remaining reliability work.

**Next Phase:**
**Phase 5 — Performance & Throughput**

---

# 📕 Distributed OMS – Saga Orchestrator

## **Full Engineering Post-Mortem (Jan 9, 2026)**

**Engineer:** Polabathina Ramcharan Teja

---

# 🧠 1. Original Working State (Before Everything Broke)

The system was originally:

```
OrderService → Saga → PaymentService
                     ↓
                 InventoryService
```

Flow was:

1. `OrderService` publishes
   `order.created`

2. `Saga` consumes
   `order.created`

3. Saga sends
   `payment.initiate`

4. `PaymentService` sends
   `payment.completed` or `payment.failed`

5. Saga consumes `payment.completed`

6. Saga sends something to Inventory

This part **worked**.

But it was **architecturally wrong**.

---

# ❌ 2. The Fundamental Design Bug

Saga was doing this:

```
payment.completed → inventory
```

Inventory was listening to:

```
payment.completed
```

This is **illegal in Saga architecture**.

Why?

Because:

* `payment.completed` is an **event**
* Inventory must never react to another service’s **events**
* Inventory must only react to **commands**

Correct design:

```
Saga → inventory.reserve.command
Inventory → inventory.reserved | inventory.unavailable
Saga → order.completed | order.failed
```

But you had:

```
payment.completed → inventory
```

That tightly coupled Inventory to Payment
and violated Saga orchestration.

This was fixed by introducing:

```
InventoryReserveCommand
```

Saga now sends:

```
inventory.reserve.command
```

Inventory replies:

```
inventory.reserved
inventory.unavailable
```

This was the **first major fix**.

---

# ⚠️ 3. After fixing Inventory Command → New Failure

After this change:

* Payment → Saga → Inventory was correct
* Inventory was replying properly

But orders still never completed.

Why?

Because **Saga was broken internally**.

---

# 💥 4. The Silent Killer: Broken Saga State Machine

This is where the nightmare began.

You had:

```java
saga.markInventoryReserved();
```

being called **before**:

```java
saga.markInventoryRequested();
```

So Saga state transitions were:

```
PAYMENT_COMPLETED → INVENTORY_RESERVED
```

instead of:

```
PAYMENT_COMPLETED → INVENTORY_REQUESTED → INVENTORY_RESERVED
```

Then Saga did:

```java
if (state != INVENTORY_REQUESTED) return;
```

So when Inventory replied:

```
inventory.reserved
```

Saga ignored it.

That caused:

* Saga never reached “COMPLETED”
* Saga never published:

    * `order.completed`
    * `order.failed`

But **no exception was thrown**.
It just silently skipped the handler.

This is the worst bug possible.

---

# 🧨 5. The Second Hidden Bug (Order Service Consumer)

While fixing Saga for hours, another disaster was hiding.

OrderService was consuming:

```
order.created
```

Instead of:

```
order.completed
order.failed
```

So even if Saga had worked,
OrderService was listening to the wrong thing.

So OrderService was:

* Never seeing completion
* Never updating order status

This made debugging impossible because **even correct events would be ignored**.

---

# 🧩 6. What Actually Took 12+ Hours

You weren’t fighting Kafka.

You were fighting:

| Layer              | What was broken            |
| ------------------ | -------------------------- |
| Saga Design        | Event vs Command violation |
| Saga State Machine | Invalid state transitions  |
| Kafka Topics       | order.completed missing    |
| Order Service      | Listening to wrong event   |
| Kafka Offsets      | Old orders unreplayable    |
| Logs               | Silent failures            |

That’s why it felt impossible.

---

# ✅ 7. What Is Correct Now

You now have:

### Proper Saga choreography

```
OrderService → order.created
Saga → payment.initiate
Payment → payment.completed
Saga → inventory.reserve.command
Inventory → inventory.reserved
Saga → order.completed
OrderService → mark CONFIRMED
```

### Correct responsibilities

| Service      | Role                       |
| ------------ | -------------------------- |
| OrderService | Emits order.created        |
| Saga         | Orchestrates               |
| Payment      | Executes payment           |
| Inventory    | Executes stock reservation |
| Kafka        | Event transport            |

### Correct event types

| Type     | Used for        |
| -------- | --------------- |
| Commands | Saga → services |
| Events   | Services → Saga |

---

# 🏁 Final Truth

This was not a Kafka issue.
This was a **distributed state machine + orchestration bug**.

You just debugged:

* Incorrect Saga modeling
* Broken state transitions
* Wrong topic wiring
* Wrong consumer wiring
* Missing topics
* Kafka offsets




## Roadmap

* **Phase 3:** Saga orchestration & distributed transactions
* **Phase 4:** Reliability (retries, DLQ, observability)
* **Phase 5:** Scalability & performance testing
* **Phase 6:** Production readiness (security, deployment, chaos testing)

---

## Status

🚧 Actively evolving
📌 Phase 1, Phase 2, Phase 3 & Phase 4 complete
