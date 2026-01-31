# EventHive - Event Management System 🐝

> A robust, scalable, and concurrency-safe backend for managing events and ticket bookings, built with **Kotlin** and **Spring Boot 3**.

EventHive is a production-grade REST API designed to handle the core operations of an event ticketing platform. It features secure authentication, role-based access control, and a high-performance booking system capable of handling concurrent requests without data inconsistencies.

---

## 🚀 Key Features

* **🔒 Secure Authentication:** JWT-based stateless authentication with custom security filters.
* **👤 Role-Based Access Control (RBAC):** Granular permissions for `USER`, `ORGANIZER`, and `ADMIN` roles.
* **⚡ Concurrency-Safe Booking:** Implements **Optimistic Locking** (`@Version`) to prevent "Lost Update" problems during high-traffic ticket sales.
* **🔄 Automatic Retries:** Uses Spring Retry to seamlessly handle race conditions without user intervention.
* **🏛️ Clean Architecture:** Separation of concerns using Domain-Driven Design (DDD) principles (Entity ↔ Domain ↔ DTO).
* **🗄️ Database Migrations:** Managed schema evolution using **Liquibase** XML changelogs.
* **📝 Audit System:** Automatic tracking of `created_by`, `updated_by`, and soft-delete capabilities.

---

## 🛠️ Tech Stack

* **Language:** Kotlin (JDK 21)
* **Framework:** Spring Boot 3.5+
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA (Hibernate)
* **Migration:** Liquibase
* **Security:** Spring Security 6 + JWT (JJWT)
* **Build Tool:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture

The project follows a **Hexagonal / Clean Architecture** approach to ensure maintainability and testability:

```text
src/main/kotlin/com/sam_the_dev/eventhive
├── api                 # Presentation Layer (Controllers, DTOs)
├── application         # Application Layer (Service Implementations, Use Cases)
├── domain              # Domain Layer (Business Logic, Interfaces, Models)
├── infrastructure      # Infrastructure Layer (Persistence, Security, Config)
└── configuration       # Spring Configuration (Beans, Security Config)
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


*Liquibase will automatically create the tables (`app_users`, `roles`, `events`, `bookings`) on startup.*

---

## 🔌 API Endpoints
> 📘 **Interactive API Documentation**
> 
>  Once the application is running, you can explore and test the API using the available documentation UIs:
> - 👉 **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
> - 👉 **ReDoc**: [http://localhost:8080/redoc.html](http://localhost:8080/redoc.html)
> - 👉 **Scalar**: [http://localhost:8080/scalar.html](http://localhost:8080/scalar.html)

---

## 🔐 Authentication

| Method | Endpoint             | Description           | Access |
|--------|----------------------|-----------------------|--------|
| `POST` | `/api/auth/register` | Register a new user   | Public |
| `POST` | `/api/auth/login`    | Login and receive JWT | Public |

---

## 👤 Users

| Method | Endpoint               | Description            | Access        |
|--------|------------------------|------------------------|---------------|
| `GET`  | `/api/user/users/{id}` | Get user details by ID | Authenticated |

---

## 🎫 Events

| Method   | Endpoint                  | Description                               | Access                              |
|----------|---------------------------|-------------------------------------------|-------------------------------------|
| `GET`    | `/api/events`             | Browse all events (paginated + filters)   | Public                              |
| `GET`    | `/api/events/{id}`        | Get event by ID                           | Public                              |
| `GET`    | `/api/events/organizer`   | Get events created by logged-in organizer | `ORGANIZER`                         |
| `POST`   | `/api/events`             | Create a new event                        | `ORGANIZER`, `ADMIN`, `SUPER_ADMIN` |
| `PUT`    | `/api/events/{id}`        | Update an event                           | `ORGANIZER`, `ADMIN`, `SUPER_ADMIN` |
| `PATCH`  | `/api/events/status/{id}` | Change event status                       | `ORGANIZER`, `ADMIN`, `SUPER_ADMIN` |
| `DELETE` | `/api/events/{id}`        | Soft delete an event                      | `ORGANIZER`, `ADMIN`, `SUPER_ADMIN` |

---

## 🎟️ Bookings (Concurrency Safe)

| Method  | Endpoint                        | Description                                   | Access                         |
|---------|---------------------------------|-----------------------------------------------|--------------------------------|
| `POST`  | `/api/bookings`                 | Create a booking                              | Authenticated                  |
| `GET`   | `/api/bookings`                 | Get my bookings (paginated)                   | Authenticated                  |
| `PATCH` | `/api/bookings/status/{id}`     | Update booking status (cancel / admin update) | Authenticated                  |
| `POST`  | `/api/bookings/webhook/payment` | Payment provider webhook                      | Public (secured via signature) |

---

## 🛡️ Admin – Role Management

| Method   | Endpoint                           | Description           | Access                 |
|----------|------------------------------------|-----------------------|------------------------|
| `PUT`    | `/api/admin/roles/assign/{userId}` | Assign role to user   | `ADMIN`, `SUPER_ADMIN` |
| `DELETE` | `/api/admin/roles/remove/{userId}` | Remove role from user | `ADMIN`, `SUPER_ADMIN` |

---

## 🧪 Testing Concurrency

The booking system is designed to handle race conditions.

1. Set `available_seats = 1`.
2. Send two simultaneous requests to `/api/bookings` from different users.
3. **Result:** One succeeds, the other automatically retries (via `@Retryable`) and fails gracefully with "Not enough seats" instead of a system crash.

---

## 📜 License

This project is licensed under the MIT License.

---

**Built with ❤️ by sam_the_dev**