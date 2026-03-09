# Architecture & Development Guidelines

Welcome to the Event Service (`core-api`) of The Hive Project. This document outlines the architectural choices, design
patterns, engineering best practices, and development workflows used in this service. It serves as a guide for current
and future developers to understand how the system is built and how to contribute to it seamlessly.

---

## 1. Architectural Patterns

### 1.1. Clean / Layered Architecture

The project strictly follows a layered architecture inspired by Clean Architecture and Domain-Driven Design (DDD). This
ensures that the core business logic is isolated from external frameworks, UI, and databases.

* **`api/` (Presentation Layer):** Contains REST Controllers, DTOs, request validation, and global exception handling.
  It translates HTTP requests into application service calls.
* **`application/` (Use Case Layer):** Contains service implementations (e.g., `EventServiceImpl`). This layer
  orchestrates business use cases, fetching data from repositories, applying business rules, and saving it back.
* **`domain/` (Business Logic Layer):** Contains core business interfaces (`EventService`), domain models, and
  domain-specific exceptions. It has no dependencies on Spring, Web, or DB frameworks.
* **`infrastructure/` (Data & External Layer):** Contains the actual database interactions (`persistence`), external API
  clients (`client`/Feign), message brokers (`notification`), and security filters (`security`).

### 1.2. Event-Driven Architecture (EDA)

The service communicates with other microservices asynchronously using **RabbitMQ**.

* Example: When a booking is successful, a `BookingSuccessEvent` is published to RabbitMQ via `NotificationProducer.kt`.
  The Notification Service consumes this to send emails/SMS, keeping the Event Service decoupled from notification
  logic.

---

## 2. Design Patterns

### 2.1. DTO (Data Transfer Object) Pattern

We strictly separate database entities (`EventEntity`) from API payloads (`EventDTO`, `CreateEventRequest`). This
prevents database schemas from leaking to the client and allows independent evolution of the API and the database.

### 2.2. Mapper Pattern (via Kotlin Extension Functions)

Instead of using heavy mapping libraries, we utilize Kotlin's extension functions for clean and idiomatic mapping.

* Example: `fun EventEntity.toDomain(userData: UserSummaryDTO): EventDTO` or `fun Event.toDTO(): EventDTO`.

### 2.3. Repository Pattern

Data access is abstracted using Spring Data JPA Repositories (e.g., `EventRepository`). The application layer interacts
with these interfaces rather than dealing directly with SQL or EntityManager, allowing easy mocking during tests.

### 2.4. Facade / Adapter Pattern

External service calls (like calling the Identity Service) are hidden behind Facade interfaces (e.g., `IdentityClient`).
This isolates the HTTP/Feign logic from the core business logic.

---

## 3. Engineering Best Practices

### 3.1. Security & Zero-Trust RBAC

* **Multi-tenant RBAC:** Security is stateless and JWT-driven. Instead of global roles, we use domain-specific
  permissions (e.g., `events:ROLE_ORGANIZER`, `movies:ROLE_USER`).
* **Method-Level Security:** Controllers are locked down using `@PreAuthorize("hasAuthority(...)")`.

### 3.2. Data Integrity & Auditing

* **Soft Deletion:** Records are never physically deleted. `AuditableEntity` includes an `isDeleted` flag and
  `deletedAt` timestamp.
* **Optimistic Locking:** Entities use the `@Version` annotation. If two users try to update the same booking
  simultaneously, JPA throws an `OptimisticLockException`, preventing lost updates.
* **Auditing:** `createdAt`, `updatedAt`, `createdBy`, and `updatedBy` are automatically tracked via the base
  `AuditableEntity`.

### 3.3. Global Exception Handling

Exceptions are thrown from the domain/application layer and intercepted by `GlobalExceptionHandler.kt` in the API layer.
This ensures consistent, standardized JSON error responses across the entire API.

### 3.4. Pagination & Search Specifications

For endpoints returning lists (e.g., `GET /api/events`), we always use Spring Data `Pageable`. Complex filtering (by
price, date, status, location) is handled dynamically using JPA Specifications (`EventSpecification.kt`).

---

## 4. Testing Strategy

* **Unit Tests (`src/test/kotlin/.../unit`):** Fast, isolated tests focusing on business logic using Mockito to mock
  repositories and clients.
* **Integration Tests (`src/test/kotlin/.../integration`):** End-to-end API tests using `MockMvc`. We use *
  *Testcontainers** to spin up real PostgreSQL and RabbitMQ Docker containers, ensuring our queries and message
  publishing work exactly as they will in production.

---

## 5. How to Add a New Feature (Developer Guide)

If you need to add a new feature (e.g., "Event Reviews"), follow this workflow:

1. **Define the Entity (`infrastructure/persistence/review/ReviewEntity.kt`):**
    * Create the JPA Entity. Inherit from `AuditableEntity`.
    * Create the `ReviewRepository` interface extending `JpaRepository`.
2. **Define the DTOs (`api/dto/ReviewDTOs.kt`):**
    * Create `ReviewDTO` (response), `CreateReviewRequest` (input validation using `@field:NotBlank`, etc.).
3. **Define the Domain Interface (`domain/review/ReviewService.kt`):**
    * Define the business operations (e.g., `fun addReview(...)`).
    * Create custom domain exceptions in `domain/review/error/`.
4. **Implement the Service (`application/review/ReviewServiceImpl.kt`):**
    * Implement the interface. Annotate methods with `@Transactional`.
    * Inject the repository and write the core business logic.
5. **Write Mappers (`api/mapper/ReviewMapper.kt`):**
    * Write Kotlin extension functions to map Entity -> Domain/DTO.
6. **Expose the API (`api/controller/ReviewController.kt`):**
    * Inject the Service.
    * Add `@RestController`, `@RequestMapping`, Swagger `@Operation` annotations.
    * Secure endpoints using `@PreAuthorize`.
7. **Write Tests:**
    * Write unit tests for `ReviewServiceImpl`.
    * Write integration tests for `ReviewController` verifying edge cases and security.
