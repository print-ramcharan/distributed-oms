# Distributed Order Management System (OMS)

A production-grade, event-driven backend system designed to demonstrate how real-world order processing systems are built, evolve, and handle failures.

This repository follows clean architecture principles and is structured to scale into a fully distributed system with Kafka-based event processing, idempotency, and fault tolerance.

---

## Current Status

✅ **Phase 1 complete**  
Foundation of the Order Service is implemented and stable.

---

## Order Service Overview

The **Order Service** is the entry point of the system.  
Its responsibility is to accept customer orders, validate input, create an order aggregate, persist it, and emit a domain event indicating that an order was created.

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
- Repository abstraction (in-memory implementation)
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

## Architecture Intent

This project intentionally follows **Clean Architecture and Domain-Driven Design**:

- Controllers handle only HTTP concerns
- Application services orchestrate use cases
- Domain layer contains pure business logic
- Infrastructure adapts external concerns (logging, persistence, messaging)
- Domain events are published via interfaces to avoid infrastructure coupling
- Dependency direction is strictly enforced

```
API → Application → Domain ← Infrastructure
```

The domain layer has **zero Spring dependencies**.

---

## Domain Events

- `OrderCreatedEvent` is emitted whenever an order is successfully created
- Events are published via a domain-defined `OrderEventPublisher`
- Current implementation logs events
- Future phases will replace this with Kafka using the outbox pattern
- No business logic changes will be required for that transition

---

## Tech Stack (Current)

- Java 17
- Spring Boot
- Maven
- REST (JSON)
- In-memory persistence
- Clean Architecture

---

## Repository Structure

```
distributed-oms/
├── pom.xml                # Parent / aggregator POM
└── services/
    └── order-service/
        ├── pom.xml
        └── src/
```

Each service is an independent Spring Boot application.

---

## Roadmap (High Level)

- Phase 2: Kafka integration and Payment Service
- Phase 3: Distributed transactions (Saga pattern)
- Phase 4: Reliability (retries, DLQ, observability)
- Phase 5: Scalability and performance testing
- Phase 6: Production readiness (security, deployment, chaos testing)

---

## Goal of This Project

This is not a UI-focused project.

The goal is to demonstrate:
- How production backend systems are structured
- How event-driven systems evolve safely
- How failures are handled without data corruption
- How clean abstractions enable scalability

---

## Status

🚧 Actively evolving  
📌 Phase 1 complete
