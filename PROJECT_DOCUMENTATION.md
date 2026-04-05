# Finance Dashboard Backend Documentation

## Project Overview

Finance Dashboard Backend is a role-based financial records management API built with Spring Boot. It provides authentication, user management, transaction management, filtering, analytics, and access control through a REST API.

This project is designed for demo and learning purposes while still following a practical backend structure with controllers, services, repositories, DTOs, validation, and centralized exception handling.

## Core Features

- User and role management
- JWT-based authentication
- Financial records CRUD operations
- Transaction filtering by date, category, and type
- Dashboard summary APIs for totals, recent transactions, and trends
- Role-based access control
- Input validation and structured error responses
- Persistent storage using H2 database

## Tech Stack

- Java 17
- Spring Boot 3.2.4
- Spring Security
- JWT (JJWT)
- Spring Data JPA
- Hibernate
- H2 Database
- Lombok
- Swagger / OpenAPI
- Maven

## Architecture

The application follows a layered architecture:

- `controller`: REST endpoints
- `service`: business logic
- `repository`: database access
- `dto`: request and response models
- `security`: JWT token generation and validation
- `config`: startup, security, and OpenAPI configuration
- `exception`: centralized error handling

## Authentication and Authorization

Authentication is handled with JWT. Users log in through `/api/auth/login` and receive a token. This token must be sent in the `Authorization` header for protected routes.

Example:

```http
Authorization: Bearer <token>
```

### Default Demo Users

The application seeds demo users automatically on first startup:

- `admin / admin123` -> ADMIN
- `analyst / analyst123` -> ANALYST
- `viewer / viewer123` -> VIEWER

## Role-Based Access Control

### ADMIN

- Can manage users
- Can create, update, and delete transactions
- Can view dashboard data
- Can view all transactions

### ANALYST

- Read-only access to transactions
- Read-only access to dashboard data

### VIEWER

- Read-only access to transactions
- Read-only access to dashboard data

## Main API Modules

### 1. Authentication

- `POST /api/auth/login`

Authenticates a user and returns a JWT token.

### 2. Users

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

These endpoints are restricted to ADMIN users.

### 3. Transactions

- `GET /api/transactions`
- `GET /api/transactions/{id}`
- `POST /api/transactions`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

Supports filtering with optional query parameters such as:

- `type`
- `category`
- `startDate`
- `endDate`
- `page`
- `size`

### 4. Dashboard

- `GET /api/dashboard/summary`

Returns:

- total income
- total expenses
- net balance
- current month metrics
- category totals
- monthly trends
- recent transactions

## Database Design

The project uses an H2 file-based database for simple demo persistence.

### Main Tables

#### `users`

- id
- username
- email
- password
- fullName
- role
- status
- createdAt
- updatedAt

#### `transactions`

- id
- amount
- type
- category
- date
- notes
- deleted
- createdBy
- createdAt
- updatedAt

## Technical Decisions and Trade-offs

- Spring Boot was chosen for its mature Java ecosystem and clean support for REST APIs, validation, and security.
- JWT-based stateless authentication keeps the backend simple and frontend-friendly.
- H2 file-based storage was chosen to avoid external database setup and make the project easy to run for demos.
- The trade-off of H2 is that it is better suited for local use and demos than production-scale deployment.
- Transactions use soft delete so financial records are not permanently removed from history.
- A single dashboard summary endpoint reduces frontend API calls but returns a larger payload.
- Analyst and Viewer roles currently share read-only capabilities, which keeps the first version simple and easy to extend later.

## Validation and Error Handling

The application uses Jakarta Bean Validation for request validation. Invalid requests return structured error responses.

Handled error scenarios include:

- invalid request payloads
- missing required fields
- duplicate username or email
- bad credentials
- unauthorized access
- forbidden role access
- resource not found
- unexpected server errors

## API Documentation

Swagger UI is available for testing endpoints interactively.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## How to Run Locally

### Prerequisites

- Java 17 or higher

### Run Command

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
.\mvnw.cmd spring-boot:run
```

The app runs on:

```text
http://localhost:8080
```

## Testing

Run all tests:

```bash
./mvnw test
```

On Windows:

```bash
.\mvnw.cmd test
```

## Deployment Notes

- The project is backend-only.
- It is suitable for demo deployment.
- H2 file storage is convenient for demos but not ideal for production hosting.
- For production use, PostgreSQL or MySQL would be a better choice.
- Docker support was added to make hosting easier on platforms that support Docker-based deployment.

## Live Demo Notes

For demo purposes, the project can be exposed through a tunnel from a local machine or deployed to a cloud platform that supports Java or Docker workloads.

## Conclusion

This project demonstrates a complete backend for finance record processing with authentication, authorization, CRUD operations, reporting APIs, validation, and a clean layered structure. It is a strong demo project for showcasing backend development with Spring Boot and security best practices.
