# Hive-Event (Core API) - Event & Booking Engine 🐝

> The high-performance, concurrency-safe core microservice for managing events, multi-tier ticketing, and live attendee check-ins, built with **Kotlin** and **Spring Boot 3**.

Hive-Event is the central domain engine of the Hive platform. Operating within a microservices architecture, it delegates authentication to the `Hive-Identity` service while focusing entirely on the event lifecycle. It features complex ticket tiering, a high-performance booking engine with optimistic locking to prevent overselling, and asynchronous event-driven notifications via RabbitMQ.

---

### 🔗 Associated Repositories
* 👉 **[EventHive UI (Frontend)](https://github.com/Naveen2070/EventHive-UI)**
* 👉 **[Hive-Identity (Auth Service)](https://github.com/Naveen2070/Hive-Identity)**

---

## 🚀 Key Features

* **🎫 Multi-Tier Ticketing:** Support for complex event structures (e.g., "Early Bird", "VIP") with individual pricing, allocation, and validity windows.
* **⚡ Concurrency-Safe Inventory:** Implements **Optimistic Locking** and `@Retryable` backoff's to prevent "Lost Update" problems and overselling during high-traffic ticket drops.
* **📱 Smart Check-In System:** QR-code ready endpoint handling **Date Validation** (e.g., checking a Friday pass on Saturday) and **Re-entry Logic** for multi-day events.
* **🐇 Async Notifications (RabbitMQ):** Completely non-blocking notification architecture. Booking confirmations are published to the `hive.notifications` exchange.
* **🛡️ Secure S2S Communication:** Uses Spring Cloud OpenFeign with custom interceptors to generate time-sensitive **HMAC-SHA256 signatures** for secure internal data fetching from the Identity Service.
* **🗄️ Database Migrations:** Managed schema evolution using **Liquibase** (PostgreSQL compatible).

---

## 🛠️ Tech Stack

* **Language:** Kotlin (JDK 21)
* **Framework:** Spring Boot 3+
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA (Hibernate)
* **Migration:** Liquibase
* **Message Broker:** RabbitMQ
* **S2S Communication:** Spring Cloud OpenFeign
* **Build Tool:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture

The project follows a **Clean Architecture** approach tailored for microservices:

```text
src/main/kotlin/com/thehiveproject/event
├── api                 # Presentation Layer (Controllers, DTOs)
├── application         # Application Layer (Service Impl, Use Cases)
├── domain              # Domain Layer (Business Logic, Models)
├── infrastructure      # Infrastructure Layer (Persistence, Feign Clients, RabbitMQ Producers)
│   └── security        # JWT Extraction & S2S HMAC Utilities
└── configuration       # Spring Configuration (Beans, Async, RabbitMQ)

```
---
Here is the updated section. I have added a prominent warning block right at the top, added `Hive-Identity` to the prerequisites, and gently corrected the syntax in your `.env` snippet (environment files use `=` instead of `:`).

I also added a quick note in the Manual Run section to remind developers to spin up RabbitMQ alongside the database!

---

## ⚙️ Getting Started (How to Run)

> ⚠️ **IMPORTANT: Microservice Dependency**
> Hive-Event is a dependent microservice. It relies entirely on the **[Hive-Identity](https://github.com/Naveen2070/Hive-Identity)** service for JWT authentication, user registration, and S2S user data hydration. To test the full application flow, you must have the Identity Service running simultaneously.

The application is fully containerized using a multi-stage Dockerfile that builds a lightweight Alpine Linux image running JDK 21 under a secure, non-root `eventhive` user.

### Prerequisites

* **Java 21** (for manual runs)
* **Docker & Docker Compose**
* **Git**
* **Hive-Identity Service** (running locally or via Docker)

### 1. Clone the Repository

```bash
git clone https://github.com/Naveen2070/EventHive.git
cd EventHive
```

### 2. Environment Variables (`.env`)

Before running the application, create a `.env` file in the root directory. The `docker-compose.yml` expects these variables to configure the `prod` profile:

```ini
# Database
DB_USERNAME=admin
DB_PASSWORD=password

# JWT Security (Must exactly match the Identity Service secret)
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRATION_MS=86400000

# RabbitMQ Config
RABBITMQ_HOST=hive-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

# Identity Service S2S Config (Must match Identity Service exactly)
INTERNAL_SHARED_SECRET=your_super_secret_shared_key
IDENTITY_SERVICE_URL=http://identity-service:8081
```

### 3. Run via Docker Compose (Recommended)

This method spins up a PostgreSQL 17.7-alpine database and your application simultaneously. The application waits for the database health check to pass before starting.

*(Note: In a full local testing setup, you would typically run a unified `docker-compose.yml` that includes the Identity Service, Core API, API Gateway, and RabbitMQ).*

```bash
# Build and start the containers in detached mode
docker-compose up --build -d
```

* The API will be available at `http://localhost:8080`.
* The PostgreSQL database is exposed on port `5432`.

To view the logs:

```bash
docker-compose logs -f app
```

### 4. Run Manually (Local Development)

If you prefer to run the Spring Boot application directly on your host machine for debugging:

**Step A: Spin up the infrastructure (Database & RabbitMQ)**

```bash
docker run --name eventhive-db -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=password -e POSTGRES_DB=eventhive -p 5432:5432 -d postgres:17.7-alpine
docker run --name hive-rabbitmq -p 5672:5672 -p 15672:15672 -d rabbitmq:3-management
```

**Step B: Run the application via Gradle**
Ensure your local `application-dev.properties` points to `localhost:5432` for Postgres, `localhost:5672` for RabbitMQ, and `localhost:8081` for the `IDENTITY_SERVICE_URL`. Then run:

```bash
./gradlew bootRun
```

*Liquibase will automatically migrate the schema on startup.*

---
## 🔌 API Endpoints

*(Note: All endpoints require a valid JWT issued by the `Hive-Identity` service).*

### 📊 Dashboard
| Method | Endpoint                | Description                                  | Access       |
|--------|-------------------------|----------------------------------------------|--------------|
| `GET`  | `/api/dashboard/stats`  | Get organizer revenue and ticket stats       | `ORGANIZER`+ |

### 🎫 Events & Ticket Tiers
| Method   | Endpoint                      | Description                           | Access       |
|----------|-------------------------------|---------------------------------------|--------------|
| `GET`    | `/api/events`                 | Browse events (includes tiers)        | Public       |
| `GET`    | `/api/events/{id}`            | Get event details                     | Public       |
| `POST`   | `/api/events`                 | Create event (with initial tiers)     | `ORGANIZER`+ |
| `PUT`    | `/api/events/{id}`            | Update event details                  | `ORGANIZER`+ |
| `PATCH`  | `/api/events/status/{id}`     | Change event status (Draft/Published) | `ORGANIZER`+ |
| `DELETE` | `/api/events/{id}`            | Soft delete an event                  | `ORGANIZER`+ |
| `POST`   | `/api/tiers/events/{eventId}` | Add a new ticket tier to an event     | `ORGANIZER`+ |
| `GET`    | `/api/tiers/{tierId}`         | Get ticket tier details               | Auth         |
| `PUT`    | `/api/tiers/{tierId}`         | Update tier (price/allocation)        | `ORGANIZER`+ |
| `DELETE` | `/api/tiers/{tierId}`         | Delete ticket tier (if no sales)      | `ORGANIZER`+ |

### 🎟️ Bookings (Concurrency Safe)
| Method  | Endpoint                        | Description                                  | Access          |
|---------|---------------------------------|----------------------------------------------|-----------------|
| `POST`  | `/api/bookings`                 | Create a booking (specifying `ticketTierId`) | Auth            |
| `GET`   | `/api/bookings`                 | Get my bookings                              | Auth            |
| `PATCH` | `/api/bookings/status/{id}`     | Cancel/update booking status                 | Auth            |
| `POST`  | `/api/bookings/webhook/payment` | Payment provider webhook                     | Public (Signed) |

### 📱 Organizer Tools (Scanner)
| Method | Endpoint                 | Description                                  | Access       |
|--------|--------------------------|----------------------------------------------|--------------|
| `GET`  | `/api/events/organizer`  | Get events created by me                     | `ORGANIZER`  |
| `POST` | `/api/bookings/check-in` | **Smart Check-in** (Validates Date/Re-entry) | `ORGANIZER`+ |

---

## 🧪 Testing Concurrency

The booking system is designed to handle race conditions on specific **Ticket Tiers**.

1. Create an event with a tier having `available_allocation = 1`.
2. Send two simultaneous requests to `/api/bookings`.
3. **Result:** One succeeds, the other automatically retries and fails gracefully with "Insufficient Seats".

---

**Built with ❤️ by naveen**
