# Inventary Management - MySQL Setup

## Overview

This Spring Boot application uses Spring Data JPA with MySQL 8+ and Hibernate.

## Prerequisites

- Java 17+
- Maven 3.9+ (or use mvnw)
- MySQL 8+

## Database Initialization

1. Start MySQL server.
2. Create database and user access (if not already present):

```sql
CREATE DATABASE IF NOT EXISTS inventary;
CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY 'admin522';
GRANT ALL PRIVILEGES ON inventary.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

3. Verify connection settings in [src/main/resources/application.properties](src/main/resources/application.properties):

- spring.datasource.url=jdbc:mysql://localhost:3306/inventary?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
- spring.datasource.username=root
- spring.datasource.password=admin522
- spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

4. Hibernate schema management:

- spring.jpa.hibernate.ddl-auto=update
- spring.jpa.show-sql=true
- spring.jpa.properties.hibernate.format_sql=true

## Build and Run

```bash
./mvnw clean package
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

## Test Configuration

Tests are configured to use MySQL in [src/test/resources/application.properties](src/test/resources/application.properties).
If your MySQL server is not running, integration tests will fail to start.

## API and CRUD Validation

Create users first (manual/self-registration), then authenticate to get JWT token:

- POST /api/v1/auth/register
- POST /api/v1/auth/login
- Example body:

```json
{
  "email": "store.manager@example.com",
  "password": "StrongPass123",
  "fullName": "Priya Sharma",
  "role": "STORE_MANAGER"
}
```

Then login:

```json
{
  "username": "store.manager@example.com",
  "password": "StrongPass123"
}
```

Use the returned token in `Authorization` header for protected endpoints:

`Authorization: Bearer <accessToken>`

Core CRUD endpoints:

- POST /api/v1/auth/login
- POST /api/v1/auth/register
- POST /api/v1/suppliers
- GET /api/v1/suppliers
- GET /api/v1/suppliers/{id}/catalog
- GET /api/v1/products?category=GROCERY
- GET /api/v1/products?low_stock=true
- POST /api/v1/products
- GET /api/v1/products/{id}
- PATCH /api/v1/products/{id}/stock
- GET /api/v1/orders?status=SUBMITTED
- GET /api/v1/orders?supplier=1
- GET /api/v1/orders/{id}
- POST /api/v1/orders
- PATCH /api/v1/orders/{id}/status
- PATCH /api/v1/orders/{id}/receive
- GET /api/v1/stock/low-alerts
- GET /api/v1/dashboard

## Role and Access Matrix

- Public (no token):
  - POST /api/v1/auth/register
  - POST /api/v1/auth/login
  - /swagger-ui/**, /v3/api-docs/**, /actuator/health

Roles used by the system:

- STORE_MANAGER (Priya Sharma persona)
- INVENTORY_ANALYST (Raj Patel persona)
- PROCUREMENT_OFFICER (Anita Singh persona)
- WAREHOUSE_STAFF (Dev Kumar persona)

### Complete Operation-Level Access

- POST /api/v1/auth/register: Public
- POST /api/v1/auth/login: Public
- GET /api/v1/suppliers: STORE_MANAGER, PROCUREMENT_OFFICER, INVENTORY_ANALYST
- POST /api/v1/suppliers: STORE_MANAGER, PROCUREMENT_OFFICER
- GET /api/v1/suppliers/{id}/catalog: STORE_MANAGER, PROCUREMENT_OFFICER, INVENTORY_ANALYST
- GET /api/v1/products: STORE_MANAGER, INVENTORY_ANALYST, PROCUREMENT_OFFICER, WAREHOUSE_STAFF
- POST /api/v1/products: STORE_MANAGER, INVENTORY_ANALYST
- GET /api/v1/products/{id}: STORE_MANAGER, INVENTORY_ANALYST, PROCUREMENT_OFFICER, WAREHOUSE_STAFF
- PATCH /api/v1/products/{id}/stock: STORE_MANAGER, WAREHOUSE_STAFF
- GET /api/v1/orders: STORE_MANAGER, PROCUREMENT_OFFICER, INVENTORY_ANALYST
- GET /api/v1/orders/{id}: STORE_MANAGER, PROCUREMENT_OFFICER, INVENTORY_ANALYST
- POST /api/v1/orders: STORE_MANAGER, PROCUREMENT_OFFICER
- PATCH /api/v1/orders/{id}/status: STORE_MANAGER
- PATCH /api/v1/orders/{id}/receive: STORE_MANAGER, WAREHOUSE_STAFF
- GET /api/v1/stock/low-alerts: STORE_MANAGER
- GET /api/v1/dashboard: STORE_MANAGER

## End-to-End Business Flow (Product to Completion)

1. Register users

- Operation: POST /api/v1/auth/register
- Who carries it: Each user self-registers (public endpoint)

2. Login and obtain JWT

- Operation: POST /api/v1/auth/login
- Who carries it: Any registered user

3. Onboard supplier

- Operation: POST /api/v1/suppliers
- Who carries it: PROCUREMENT_OFFICER or STORE_MANAGER

4. Create product master data

- Operation: POST /api/v1/products
- Who carries it: INVENTORY_ANALYST or STORE_MANAGER

5. Monitor current product details and low stock

- Operations: GET /api/v1/products, GET /api/v1/products/{id}, GET /api/v1/stock/low-alerts
- Who carries it:
  - Product list/details: INVENTORY_ANALYST, PROCUREMENT_OFFICER, WAREHOUSE_STAFF, STORE_MANAGER
  - Low-stock alerts: STORE_MANAGER

6. Raise purchase order for replenishment

- Operation: POST /api/v1/orders
- Who carries it: PROCUREMENT_OFFICER or STORE_MANAGER

7. Track purchase order progress

- Operations: GET /api/v1/orders, GET /api/v1/orders/{id}
- Who carries it: INVENTORY_ANALYST, PROCUREMENT_OFFICER, STORE_MANAGER

8. Approve/advance PO status

- Operation: PATCH /api/v1/orders/{id}/status
- Who carries it: STORE_MANAGER

9. Receive goods in warehouse

- Operation: PATCH /api/v1/orders/{id}/receive
- Who carries it: WAREHOUSE_STAFF or STORE_MANAGER

10. Record additional stock movements (sale, adjustment, count updates)

- Operation: PATCH /api/v1/products/{id}/stock
- Who carries it: WAREHOUSE_STAFF or STORE_MANAGER

11. Review operational health and KPIs

- Operation: GET /api/v1/dashboard
- Who carries it: STORE_MANAGER

## Swagger API Testing

Swagger is enabled in this project using springdoc OpenAPI.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### How to test with Swagger

1. Start the application.
2. Open Swagger UI URL.
3. Call `POST /api/v1/auth/login` and copy `accessToken` from the response.

- If needed, first create user with `POST /api/v1/auth/register`.

4. Click Authorize and enter: `Bearer <accessToken>`
5. Use Try it out on secured endpoints.

No users are seeded automatically.

Create users manually via `POST /api/v1/auth/register` before testing secured endpoints.

Note:

- `/v3/api-docs/**` and `/swagger-ui/**` are publicly accessible.
- `/api/v1/auth/login` is public.
- Business endpoints require a valid JWT token.
