# Finance Dashboard — Spring Boot Backend

A role-based financial records management backend built with **Spring Boot 3**, **Spring Security + JWT**, and **H2** (file-backed database). Designed for clarity, correctness, and real-world extensibility.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Getting Started](#getting-started)
4. [Default Credentials](#default-credentials)
5. [Role & Access Control Model](#role--access-control-model)
6. [API Reference](#api-reference)
   - [Authentication](#authentication)
   - [Users](#users)
   - [Transactions](#transactions)
   - [Dashboard](#dashboard)
7. [Design Decisions & Assumptions](#design-decisions--assumptions)
8. [Data Model](#data-model)
9. [Error Handling](#error-handling)
10. [Running Tests](#running-tests)

---

## Tech Stack

| Concern          | Choice                                    | Reason                                        |
|------------------|-------------------------------------------|-----------------------------------------------|
| Framework        | Spring Boot 3.2 (Java 17)                 | Mature, widely used, excellent ecosystem      |
| Security         | Spring Security + JJWT 0.12              | Stateless JWT; no session management needed   |
| Persistence      | Spring Data JPA + Hibernate               | Clean repository abstraction, JPQL for aggs   |
| Database         | H2 (file-backed)                          | Zero-setup, portable, survives restarts       |
| Validation       | Jakarta Bean Validation                   | Declarative, integrates with Spring MVC       |
| Boilerplate      | Lombok                                    | Reduces noise in models and services          |

---

## Project Structure

```
src/main/java/com/finance/dashboard/
│
├── FinanceDashboardApplication.java      # Entry point
│
├── config/
│   ├── SecurityConfig.java               # Filter chain, RBAC URL rules, password encoder
│   └── DataInitializer.java              # Seeds default users + sample transactions on first run
│
├── controller/
│   ├── AuthController.java               # POST /api/auth/login
│   ├── UserController.java               # CRUD /api/users  (ADMIN only)
│   ├── TransactionController.java        # CRUD /api/transactions
│   └── DashboardController.java          # GET  /api/dashboard/summary
│
├── dto/
│   ├── request/                          # Inbound payloads (validated with Bean Validation)
│   └── response/                         # Outbound shapes (never expose raw entities)
│
├── exception/
│   ├── ResourceNotFoundException.java    # 404
│   ├── BusinessException.java            # 409 (e.g. duplicate username)
│   └── GlobalExceptionHandler.java       # Maps all exceptions → structured JSON
│
├── model/
│   ├── User.java                         # JPA entity
│   ├── Transaction.java                  # JPA entity (with soft-delete flag)
│   └── enums/  Role · TransactionType · UserStatus
│
├── repository/
│   ├── UserRepository.java               # Standard JPA + status filter
│   └── TransactionRepository.java        # Custom JPQL: filters, aggregations, trends
│
├── security/
│   ├── JwtTokenProvider.java             # Generate + validate JWT
│   ├── JwtAuthenticationFilter.java      # OncePerRequestFilter reads Bearer token
│   └── UserDetailsServiceImpl.java       # Loads user + checks ACTIVE status
│
└── service/
    ├── AuthService.java                  # Authenticate → issue token
    ├── UserService.java                  # User CRUD business logic
    ├── TransactionService.java           # Transaction CRUD, soft delete
    └── DashboardService.java             # Aggregation & analytics
```

---

## Getting Started

### Prerequisites

- **Java 17+** (tested on Java 21)

> A Maven wrapper is included, so you do not need to install Maven separately.

### Run

```bash
# Clone / unzip the project
cd finance-dashboard

# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The server starts at **http://localhost:8080**.

H2 data is persisted to `./data/financedb.mv.db` — it survives application restarts.

### If You See "Database May Be Already In Use"

That means another copy of the backend is already running and holding the H2 file lock.

- Stop the old backend process and start again
- Or reuse the running instance at `http://localhost:8080`

### H2 Console (optional)

Browse the database at: **http://localhost:8080/h2-console**

| Field    | Value                                           |
|----------|-------------------------------------------------|
| JDBC URL | `jdbc:h2:file:./data/financedb`                 |
| Username | `sa`                                            |
| Password | *(leave blank)*                                 |

### API Documentation

- Swagger UI: **http://localhost:8080/swagger-ui/index.html**
- OpenAPI JSON: **http://localhost:8080/v3/api-docs**

---

## Default Credentials

Three users are seeded automatically on the first startup:

| Username   | Password     | Role     |
|------------|--------------|----------|
| `admin`    | `admin123`   | ADMIN    |
| `analyst`  | `analyst123` | ANALYST  |
| `viewer`   | `viewer123`  | VIEWER   |

Sample transactions (income + expenses across several categories and months) are also seeded so the dashboard is immediately populated.

---

## Role & Access Control Model

Access control is enforced at **two layers**:

1. **URL-level** — `SecurityConfig.java` uses `requestMatchers` to block entire HTTP method/path patterns by role.
2. **Service-level** — The security context is consulted inside services when the acting user's identity is relevant (e.g. `createdBy` stamping).

| Action                              | VIEWER | ANALYST | ADMIN |
|-------------------------------------|:------:|:-------:|:-----:|
| Login                               | ✅     | ✅      | ✅    |
| View transactions (list + detail)   | ✅     | ✅      | ✅    |
| View dashboard summary              | ✅     | ✅      | ✅    |
| Create transaction                  | ❌     | ❌      | ✅    |
| Update transaction                  | ❌     | ❌      | ✅    |
| Delete (soft) transaction           | ❌     | ❌      | ✅    |
| List / view users                   | ❌     | ❌      | ✅    |
| Create / update / delete users      | ❌     | ❌      | ✅    |

> **Analyst vs Viewer:** Both currently share read-only access. The model is designed so that analyst-only endpoints (e.g. export, advanced insights) can be added easily by gating them with `hasRole("ANALYST")`.

---

## API Reference

All protected endpoints require:
```
Authorization: Bearer <token>
```

All responses follow the envelope:
```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2025-04-02T10:00:00"
}
```

---

### Authentication

#### `POST /api/auth/login`

Authenticate and receive a JWT.

**Request body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200`:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "username": "admin",
    "fullName": "System Administrator",
    "role": "ADMIN"
  }
}
```

**Error `401`** — wrong credentials  
**Error `400`** — missing fields

---

### Users

> All endpoints require `ADMIN` role.

#### `GET /api/users?page=0&size=20&status=ACTIVE`

Lists all users (paginated). `status` filter is optional (`ACTIVE` | `INACTIVE`).

#### `GET /api/users/{id}`

Returns a single user.

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@finance.com",
    "fullName": "System Administrator",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2025-04-01T09:00:00",
    "updatedAt": "2025-04-01T09:00:00"
  }
}
```

#### `POST /api/users`

Creates a new user.

**Request body:**
```json
{
  "username": "jane",
  "email": "jane@company.com",
  "password": "secure123",
  "fullName": "Jane Doe",
  "role": "ANALYST"
}
```

**Validations:**
- `username` — 3–60 chars, alphanumeric + underscore, unique
- `email` — valid format, unique
- `password` — min 6 chars
- `role` — one of `VIEWER`, `ANALYST`, `ADMIN`

**Response `201`** — created user (no password field)  
**Error `409`** — duplicate username or email

#### `PUT /api/users/{id}`

Partial update — only non-null fields are applied.

```json
{
  "role": "ADMIN",
  "status": "INACTIVE"
}
```

#### `DELETE /api/users/{id}`

Permanently removes a user.

---

### Transactions

#### `GET /api/transactions`

Paginated list with optional filters. All query params are optional.

| Param       | Type     | Example           | Description                         |
|-------------|----------|-------------------|-------------------------------------|
| `type`      | enum     | `INCOME`          | `INCOME` or `EXPENSE`               |
| `category`  | string   | `Rent`            | Case-insensitive partial match      |
| `startDate` | date     | `2025-01-01`      | ISO date — inclusive lower bound    |
| `endDate`   | date     | `2025-03-31`      | ISO date — inclusive upper bound    |
| `page`      | int      | `0`               | Zero-based page index               |
| `size`      | int      | `20`              | Max 100 per request                 |

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "amount": 85000.00,
        "type": "INCOME",
        "category": "Salary",
        "date": "2025-03-30",
        "notes": "Monthly salary",
        "createdBy": "admin",
        "createdAt": "...",
        "updatedAt": "..."
      }
    ],
    "totalElements": 13,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

#### `GET /api/transactions/{id}`

Returns a single non-deleted transaction.

#### `POST /api/transactions` *(ADMIN)*

```json
{
  "amount": 25000.00,
  "type": "EXPENSE",
  "category": "Rent",
  "date": "2025-04-01",
  "notes": "Monthly rent payment"
}
```

**Validations:**
- `amount` — > 0, max 13 integer digits, 2 decimal places
- `type` — `INCOME` or `EXPENSE`
- `category` — required, max 80 chars
- `date` — required, not future
- `notes` — optional, max 500 chars

#### `PUT /api/transactions/{id}` *(ADMIN)*

Full replacement of all fields (same schema as POST).

#### `DELETE /api/transactions/{id}` *(ADMIN)*

Soft-deletes the transaction (`deleted=true`). The record is retained in the database and excluded from all queries.

---

### Dashboard

#### `GET /api/dashboard/summary`

Returns the full analytics payload in a single call.

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "totalIncome": 183500.00,
    "totalExpenses": 67100.00,
    "netBalance": 116400.00,

    "currentMonthIncome": 100500.00,
    "currentMonthExpenses": 34200.00,
    "currentMonthNet": 66300.00,

    "categoryTotals": [
      { "category": "Salary",    "type": "INCOME",  "total": 160000.00 },
      { "category": "Rent",      "type": "EXPENSE", "total": 47000.00  },
      { "category": "Groceries", "type": "EXPENSE", "total": 9700.00   }
    ],

    "monthlyTrends": [
      {
        "year": 2025, "month": 3, "monthLabel": "Mar 2025",
        "income": 93000.00, "expenses": 44600.00, "net": 48400.00
      },
      {
        "year": 2025, "month": 4, "monthLabel": "Apr 2025",
        "income": 100500.00, "expenses": 34200.00, "net": 66300.00
      }
    ],

    "recentTransactions": [
      { "id": 9, "amount": 999.00, "type": "EXPENSE", "category": "Subscriptions", ... }
    ]
  }
}
```

---

## Design Decisions & Assumptions

### Soft Delete on Transactions
Financial records should never be permanently erased — audit trails matter. Transactions have a `deleted` boolean flag. All queries filter `deleted = false` by default. This also protects referential integrity when transactions are linked to reporting periods.

Users are hard-deleted because they are admin-managed and low-volume, with no direct audit requirement.

### Single Dashboard Endpoint
The summary endpoint returns a comprehensive payload in one call rather than multiple fine-grained endpoints (e.g. `/totals`, `/trends`, `/recent`). This reduces round trips for the most common dashboard load case. If the payload grows, it can be split later.

### Uniform API Response Envelope
Every response (success and error) uses `ApiResponse<T>` with `success`, `message`, `data`, and `timestamp`. This makes frontend error handling consistent and predictable.

### No Separate Permission Table
Roles are stored as an enum on the `User` entity. A separate `permissions` table would be appropriate for fine-grained, dynamic permissions. Given the three fixed roles with clear boundaries, enum-based RBAC is simpler and sufficient.

### H2 File-Backed Database
H2 in file mode (`jdbc:h2:file:...`) gives the simplicity of an embedded database while persisting data across restarts. Migrating to PostgreSQL or MySQL requires only changing the `datasource` URL and adding the corresponding driver dependency — all JPA and repository code remains identical.

### JWT Expiry
Tokens expire after **24 hours** (configurable via `app.jwt.expiration-ms`). No refresh token mechanism is implemented; for production this would be needed.

### Transaction `createdBy` Stamp
The creating user is resolved from the Spring Security context inside the service layer (not passed from the controller). This prevents clients from spoofing the author field.

### Page Size Cap
The transaction list endpoint silently clamps `size` to 100 to protect against accidentally fetching the entire table.

---

## Data Model

```
users
─────────────────────────────────────────────────────
id          BIGINT PK AUTO_INCREMENT
username    VARCHAR(60) UNIQUE NOT NULL
email       VARCHAR(120) UNIQUE NOT NULL
password    VARCHAR NOT NULL          ← BCrypt hash
full_name   VARCHAR(80) NOT NULL
role        VARCHAR(20) NOT NULL      ← VIEWER | ANALYST | ADMIN
status      VARCHAR(20) NOT NULL      ← ACTIVE | INACTIVE
created_at  TIMESTAMP NOT NULL
updated_at  TIMESTAMP NOT NULL


transactions
─────────────────────────────────────────────────────
id          BIGINT PK AUTO_INCREMENT
amount      DECIMAL(15,2) NOT NULL
type        VARCHAR(20) NOT NULL      ← INCOME | EXPENSE
category    VARCHAR(80) NOT NULL
date        DATE NOT NULL
notes       VARCHAR(500)
deleted     BOOLEAN NOT NULL DEFAULT false
created_by  BIGINT FK → users(id)
created_at  TIMESTAMP NOT NULL
updated_at  TIMESTAMP NOT NULL

Indexes: type, category, date, deleted
```

---

## Error Handling

| Scenario                      | HTTP Status | `success` |
|-------------------------------|-------------|-----------|
| Successful operation          | 200 / 201   | `true`    |
| Validation failure            | 400         | `false`   |
| Bad credentials               | 401         | `false`   |
| Inactive account              | 401         | `false`   |
| Insufficient role             | 403         | `false`   |
| Resource not found            | 404         | `false`   |
| Duplicate username/email      | 409         | `false`   |
| Unexpected server error       | 500         | `false`   |

Validation errors include a `data` map of `fieldName → errorMessage` for easy form highlighting:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than zero",
    "date":   "Date cannot be in the future"
  }
}
```

---

## Running Tests

```bash
# All tests (Windows)
.\mvnw.cmd test

# All tests (macOS / Linux)
./mvnw test

# Specific test class (Windows)
.\mvnw.cmd -Dtest=TransactionServiceTest test
.\mvnw.cmd -Dtest=AuthControllerTest test
```

Test coverage includes:

| Test file                     | Type        | What it covers                                    |
|-------------------------------|-------------|---------------------------------------------------|
| `AuthControllerTest`          | Integration | Login success, bad credentials, missing fields    |
| `TransactionControllerTest`   | Integration | RBAC enforcement, filtering, validation, 401/403  |
| `DashboardControllerTest`     | Integration | All roles can read summary, unauthenticated block |
| `TransactionServiceTest`      | Unit        | CRUD logic, soft-delete, page size cap, not found |
| `UserServiceTest`             | Unit        | Create/update/delete, duplicate checks, not found |

Integration tests use an in-memory H2 instance (via `application-test.yml`) with `create-drop` DDL so each test run starts clean.
#   F i n a n c e - D a t a - P r o c e s s i n g - a n d - A c c e s s - C o n t r o l - B a c k e n d  
 "# Finance-Data-Processing-and-Access-Control-Backend" 
