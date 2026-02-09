# EventHive - Event Management System 🐝

> A robust, scalable, and concurrency-safe backend for managing events, multi-tier ticketing, and live attendee check-ins, built with **Kotlin** and **Spring Boot 3**.

EventHive is a production-grade REST API designed to handle the full lifecycle of an event platform. It features secure authentication, complex ticket tiering (e.g., VIP vs. General), an event-driven notification system, and a high-performance booking engine capable of handling concurrent requests without data inconsistencies.

---

## 🚀 Key Features

* **🔒 Secure Authentication:** JWT-based stateless authentication with custom security filters.
* **👤 Advanced User Management:** Full profile management including secure **Password Reset** (Forgot/Reset flow), Profile Updates, and Role Management.
* **🎫 Multi-Tier Ticketing:** Support for complex event structures (e.g., "Early Bird", "VIP", "Weekend Pass") with individual pricing, allocation, and validity windows.
* **⚡ Concurrency-Safe Inventory:** Implements **Optimistic Locking** on specific ticket tiers to prevent "Lost Update" problems during high-traffic sales.
* **📱 Smart Check-In System:** QR-code ready endpoint handling **Date Validation** (e.g., checking a Friday pass on Saturday) and **Re-entry Logic** for multi-day events.
* **🔔 Event-Driven Notifications:** Asynchronous email delivery system (Booking Confirmations, Security Alerts) using Spring Events to keep the API response time fast.
* **🔄 Automatic Retries:** Uses Spring Retry to seamlessly handle race conditions during booking without user intervention.
* **🗄️ Database Migrations:** Managed schema evolution using **Liquibase** (PostgreSQL compatible).

---

## 🛠️ Tech Stack

* **Language:** Kotlin (JDK 21)
* **Framework:** Spring Boot 3.5+
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA (Hibernate)
* **Migration:** Liquibase
* **Security:** Spring Security 6 + JWT (JJWT)
* **Notifications:** Spring Mail (JavaMailSender) + Spring Events
* **Build Tool:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture

The project follows a **Hexagonal / Clean Architecture** approach, enhanced with an **Event-Driven** layer for notifications:

```text
src/main/kotlin/com/sam_the_dev/eventhive
├── api                 # Presentation Layer (Controllers, DTOs)
├── application         # Application Layer (Service Impl, Use Cases)
├── domain              # Domain Layer (Business Logic, Models, Events)
│   └── event           # Domain Events (e.g., BookingSuccessEvent)
├── infrastructure      # Infrastructure Layer (Persistence, Email, Security)
│   └── notification    # Async Event Listeners & Email Service
└── configuration       # Spring Configuration (Beans, Security, Async)

```

---

## ⚙️ Getting Started

### Prerequisites

* Java 21 or higher
* Docker (for PostgreSQL)
* Git

### 1. Clone the Repository

```bash
git clone https://github.com/Naveen2070/EventHive.git
cd EventHive
```

### 2. Run with Docker (Recommended)

The easiest way to run the application (App + Database) is using Docker Compose:

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

### 3. Manual Setup (Dev Mode)

If you want to run it locally for development:

##### 1. Spin up the database:

```bash
docker run --name eventhive-db -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=password -e POSTGRES_DB=eventhive -p 5432:5432 -d postgres:15-alpine
```

##### 2. Run the app:

```bash
./gradlew bootRun
```

*Liquibase will automatically migrate the schema (including the new `ticket_tiers` and `password_reset_tokens` tables) on startup.*

---

## 🔌 API Endpoints

> 📘 **Interactive API Documentation**
> Once the application is running, you can explore and test the API using the available documentation UIs:
> * 👉 **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](https://www.google.com/search?q=http://localhost:8080/swagger-ui/index.html)
> * 👉 **ReDoc**: [http://localhost:8080/redoc.html](https://www.google.com/search?q=http://localhost:8080/redoc.html)
> * 👉 **Scalar**: [http://localhost:8080/scalar.html](https://www.google.com/search?q=http://localhost:8080/scalar.html)
>
>

---

### 🔐 Authentication & Security

| Method | Endpoint                         | Description                  | Access |
|--------|----------------------------------|------------------------------|--------|
| `POST` | `/api/auth/register`             | Register a new user          | Public |
| `POST` | `/api/auth/login`                | Login and receive JWT        | Public |
| `POST` | `/api/user/forgot-password`      | Request password reset email | Public |
| `POST` | `/api/user/reset-password`       | Reset password via token     | Public |
| `POST` | `/api/user/change-password/{id}` | Change password (logged in)  | Auth   |

---

### 👤 User Management

| Method | Endpoint               | Description            | Access        |
|--------|------------------------|------------------------|---------------|
| `GET`  | `/api/user/users/{id}` | Get user profile       | Authenticated |
| `PUT`  | `/api/user/{id}`       | Update profile details | Owner Only    |

---

### 🎫 Events & Ticket Tiers

| Method   | Endpoint                      | Description                           | Access       |
|----------|-------------------------------|---------------------------------------|--------------|
| `GET`    | `/api/events`                 | Browse events (includes tiers)        | Public       |
| `GET`    | `/api/events/{id}`            | Get event details                     | Public       |
| `POST`   | `/api/events`                 | Create event (with initial tiers)     | `ORGANIZER`+ |
| `POST`   | `/api/tiers/events/{eventId}` | **Add a new ticket tier**             | `ORGANIZER`+ |
| `PUT`    | `/api/tiers/{tierId}`         | **Update tier** (price/allocation)    | `ORGANIZER`+ |
| `DELETE` | `/api/tiers/{tierId}`         | Delete tier (if no sales)             | `ORGANIZER`+ |
| `PATCH`  | `/api/events/status/{id}`     | Change event status (Draft/Published) | `ORGANIZER`+ |

---

### 🎟️ Bookings (Concurrency Safe)

| Method  | Endpoint                        | Description                                  | Access          |
|---------|---------------------------------|----------------------------------------------|-----------------|
| `POST`  | `/api/bookings`                 | Create a booking (specifying `ticketTierId`) | Auth            |
| `GET`   | `/api/bookings`                 | Get my bookings                              | Auth            |
| `PATCH` | `/api/bookings/status/{id}`     | Cancel booking (Restores Tier inventory)     | Auth            |
| `POST`  | `/api/bookings/webhook/payment` | Payment provider webhook                     | Public (Signed) |

---

### 📱 Organizer Tools (Scanner)

| Method | Endpoint                 | Description                                  | Access       |
|--------|--------------------------|----------------------------------------------|--------------|
| `POST` | `/api/bookings/check-in` | **Smart Check-in** (Validates Date/Re-entry) | `ORGANIZER`+ |
| `GET`  | `/api/events/organizer`  | Get events created by me                     | `ORGANIZER`  |

---

### 🛡️ Admin – Role Management

| Method   | Endpoint                           | Description           | Access                 |
|----------|------------------------------------|-----------------------|------------------------|
| `PUT`    | `/api/admin/roles/assign/{userId}` | Assign role to user   | `ADMIN`, `SUPER_ADMIN` |
| `DELETE` | `/api/admin/roles/remove/{userId}` | Remove role from user | `ADMIN`, `SUPER_ADMIN` |

---

## 🧪 Testing Concurrency

The booking system is designed to handle race conditions on specific **Ticket Tiers**.

1. Create an event with a "VIP" tier having `available_allocation = 1`.
2. Send two simultaneous requests to `/api/bookings` targeting that VIP Tier.
3. **Result:** One succeeds, the other automatically retries (via `@Retryable`) and fails gracefully with "Insufficient Seats" once the lock is released.

---

## 📜 License

This project is licensed under the MIT License.

---

**Built with ❤️ by naveen**