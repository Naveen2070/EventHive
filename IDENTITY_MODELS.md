# Identity Service - Data Models Reference

This document provides a comprehensive reference of the Entities and Data Transfer Objects (DTOs) used in the Identity
Service. This can be used by other services in the Hive Project to understand the data structures for communication and
integration.

---

## 1. Entities

Entities represent the database schema and persistent data.

### `BaseEntity`

_Package: `com.thehiveproject.identity_service.common.entity`_

Common base class for all entities, providing auditing and soft-delete capabilities.

| Field       | Type             | Description                                         |
|:------------|:-----------------|:----------------------------------------------------|
| `id`        | `Long?` / `Int?` | Unique identifier (TSID for Long, Identity for Int) |
| `createdBy` | `Long?`          | ID of the user who created the record               |
| `updatedBy` | `Long?`          | ID of the user who last updated the record          |
| `createdAt` | `Instant`        | Timestamp of creation                               |
| `updatedAt` | `Instant`        | Timestamp of last update                            |
| `version`   | `Long`           | Optimistic locking version                          |
| `active`    | `Boolean`        | Whether the record is active                        |
| `deleted`   | `Boolean`        | Whether the record is soft-deleted                  |
| `deletedAt` | `Instant?`       | Timestamp of soft-deletion                          |

### `User`

_Package: `com.thehiveproject.identity_service.user.entity`_

| Field          | Type                   | Description                              |
|:---------------|:-----------------------|:-----------------------------------------|
| `id`           | `Long?`                | Primary Key (TSID)                       |
| `email`        | `String`               | Unique email address                     |
| `passwordHash` | `String`               | Hashed password                          |
| `fullName`     | `String`               | User's full name                         |
| `roles`        | `MutableSet<UserRole>` | One-to-many relationship with `UserRole` |

### `Role`

_Package: `com.thehiveproject.identity_service.user.entity`_

| Field  | Type     | Description                              |
|:-------|:---------|:-----------------------------------------|
| `id`   | `Int?`   | Primary Key (Auto-increment)             |
| `name` | `String` | Unique role name (e.g., "USER", "ADMIN") |

### `UserRole`

_Package: `com.thehiveproject.identity_service.user.entity`_

Join table entity for User-Role relationship with Domain support.

| Field    | Type     | Description                                           |
|:---------|:---------|:------------------------------------------------------|
| `id`     | `Long?`  | Primary Key (TSID)                                    |
| `user`   | `User`   | Reference to User                                     |
| `role`   | `Role`   | Reference to Role                                     |
| `domain` | `String` | Specific domain this role applies to (e.g., "events") |

---

## 2. Data Transfer Objects (DTOs)

DTOs are used for API requests and responses.

### Auth DTOs

#### `AuthResponse`

Response sent after successful login or token refresh.

| Field          | Type     | Description                                                    |
|:---------------|:---------|:---------------------------------------------------------------|
| `token`        | `String` | JWT access token (Contains `domains` and `permissions` claims) |
| `refreshToken` | `String` | Refresh token UUID                                             |
| `email`        | `String` | Authenticated user email                                       |

#### `LoginRequest`

Payload for user authentication.

| Field      | Type     | Description        |
|:-----------|:---------|:-------------------|
| `email`    | `String` | User email address |
| `password` | `String` | User password      |

#### `RegisterRequest` / `CreateUserRequest`

Payload for user registration or admin creation.

| Field         | Type                  | Description                                                    |
|:--------------|:----------------------|:---------------------------------------------------------------|
| `fullName`    | `String`              | User's full name                                               |
| `email`       | `String`              | User's email                                                   |
| `password`    | `String`              | User's password (min 8 chars)                                  |
| `domainRoles` | `Map<String, String>` | Map of domains to requested roles (e.g., `{"events": "USER"}`) |

---

### User DTOs

#### `UserResponse`

Standard user profile response.

| Field         | Type                        | Description             |
|:--------------|:----------------------------|:------------------------|
| `id`          | `String`                    | TSID as String          |
| `fullName`    | `String`                    | User's full name        |
| `email`       | `String`                    | User's email            |
| `domainRoles` | `Map<String, List<String>>` | Roles grouped by domain |
| `createdAt`   | `Instant`                   | Creation timestamp      |
| `isActive`    | `Boolean`                   | Account status          |

#### `UserDto`

Comprehensive DTO containing full entity state.

| Field   | Type               | Description                            |
|:--------|:-------------------|:---------------------------------------|
| `id`    | `String?`          | ID                                     |
| `email` | `String?`          | Email                                  |
| `roles` | `Set<UserRoleDto>` | Nested role details including `domain` |

#### `UserRoleDto` (Nested in `UserDto`)

| Field      | Type      | Description       |
|:-----------|:----------|:------------------|
| `roleName` | `String?` | Role name         |
| `domain`   | `String?` | Associated domain |
