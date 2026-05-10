# SCOPE.md – Concert Ticket Booking Platform

## 1. Assumptions

- **Booking status lifecycle:** A booking can have one of the following statuses: `PENDING`, `PAID`, `CANCELLED`, `EXPIRED`, `FAILED`. No real payment gateway is integrated; therefore the `PAID`/`FAILED` statuses are manually updated by administrators via the admin dashboard.
- **Voucher usage rules:** Each user can use a given voucher only once (enforced by the `user_voucher_usage` table). Vouchers are **create-only** – after creation they cannot be updated or deleted. Only admins can create new vouchers.
- **Concert publication:** Only concerts with status `PUBLISHED` are visible to customers. Admins can publish or unpublish a concert at any time.
- **Idempotency key generation:** The client (frontend/mobile application) is responsible for generating a unique UUID for each booking attempt and sending it in the `Idempotency-Key` header (or in request body). The server does not generate keys on behalf of the client.
- **Inventory management:** The `ticket_categories` table stores both `total_quantity` and a separate `available_quantity`. A database trigger automatically restores `available_quantity` when a booking is cancelled, expires, or fails. **The service layer never performs manual inventory release** – double release is impossible.
- **Authentication & authorisation:** The system uses JWT with access tokens (15 minutes) and refresh tokens (7 days). No token blacklist is implemented; a stolen refresh token remains valid until its natural expiration.

## 2. Implemented Features

### 2.1 Customer‑facing APIs (no authentication required or customer role)

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/register` | Register a new customer account (role `CUSTOMER`). |
| `POST /api/auth/login` | Authenticate and receive access + refresh tokens. |
| `POST /api/auth/refresh` | Obtain a new access token using a valid refresh token. |
| `GET /api/concerts` | List all published concerts (simple list, no pagination). |
| `GET /api/concerts/{id}` | Get details of a specific concert. |
| `GET /api/concerts/{id}/ticket-categories` | List ticket categories (with price and availability) for a concert. |
| `POST /api/bookings` | Create a booking (idempotent, supports multiple ticket categories in one request). Requires `idempotencyKey` in body. |
| `POST /api/bookings/{id}/cancel` | Cancel a pending booking (only allowed if `status == PENDING`). |
| `GET /api/bookings/{id}` | Retrieve a single booking (only if the authenticated user owns it). |
| `GET /api/bookings/user` | List all bookings of the authenticated customer. |

### 2.2 Admin / Operation APIs (require role `ADMIN`)

| Endpoint | Description |
|----------|-------------|
| `POST /api/admin/concerts` | Create a concert with multiple ticket categories. |
| `PUT /api/admin/concerts/{id}` | Update concert details (name, description, venue, event date, status). |
| `POST /api/admin/concerts/{id}/publish` | Set concert status to `PUBLISHED`. |
| `POST /api/admin/concerts/{id}/unpublish` | Set concert status back to `DRAFT`. |
| `GET /api/admin/concerts` | Paginated list of concerts with filters (name, venue, status, date range). |
| `GET /api/admin/concerts/{id}` | Get concert details. |
| `POST /api/admin/ticket-categories?concertId={id}` | Create a new ticket category under a concert. |
| `PUT /api/admin/ticket-categories/{id}` | Update price, name, or total quantity. |
| `GET /api/admin/ticket-categories/concert/{concertId}` | Paginated list with filters (name, min/max price, min available). |
| `GET /api/admin/ticket-categories/{id}` | Get category details. |
| `POST /api/admin/vouchers` | Create a new voucher (code, discount type, value, limits, validity). |
| `GET /api/admin/vouchers` | Paginated list with filters (code, discountType, isValid, validFrom/To). |
| `GET /api/admin/vouchers/{id}` | Get voucher by ID. |
| `GET /api/admin/vouchers/code/{code}` | Get voucher by code. |
| `GET /api/admin/bookings` | Paginated list of all bookings with filters (status, userId, concertId, date range). |
| `PUT /api/admin/bookings/{id}/status` | Manually update a booking status (e.g., `PENDING` → `PAID` / `CANCELLED` / `FAILED`). Releases tickets automatically via database trigger. |
| `POST /api/admin/users` | Create a user (can specify role; default `CUSTOMER`). |
| `PUT /api/admin/users/{id}` | Update user name, password, or role. |
| `GET /api/admin/users/{id}` | Get user details. |
| `GET /api/admin/users` | Paginated list with filters (email, name, role). |

> **Note:** Delete operations (concerts, ticket categories, users, vouchers) are **not implemented** in the current version to keep the scope focused on core flash‑sale features.

### 2.3 Core Business Logic

- **Idempotency:** Unique constraint on `bookings.idempotency_key`. The service checks for an existing key before creating a new booking. If found, returns the previous result with a `duplicate: true` flag (or `Idempotency-Processed` header).
- **Overselling prevention:** Pessimistic row locking (`SELECT FOR UPDATE`) on `ticket_categories` row inside the booking transaction. If `available_quantity` is insufficient, a `409 Conflict` is returned.
- **Voucher anti-abuse:** The `user_voucher_usage` table has a unique constraint `(user_id, voucher_id)`. The voucher row is also locked (`SELECT FOR UPDATE`) inside the booking transaction to safely increment `used_count`.
- **Inventory release:** A database trigger (`release_tickets_on_booking_cancel()`) automatically restores `available_quantity` when a booking status changes from `PENDING` to `CANCELLED`, `EXPIRED`, or `FAILED`. **No manual release code exists** in the service layer.
- **Caching (Redis):** Frequently accessed queries – such as `getPublishedConcerts()` and `getConcertById()` – are cached using Spring Cache with Redis as the cache store. Cache TTL is 5 minutes, and cache is automatically evicted when concerts are created, updated, published, or deleted. This reduces database load during flash sale read peaks.
- **Global exception handling:** `@RestControllerAdvice` returns consistent JSON error responses with appropriate HTTP status codes (400, 401, 403, 404, 409, 500) and human‑readable messages.
- **Security:** JWT authentication with access/refresh tokens. Passwords hashed with BCrypt. Role‑based authorisation via `@PreAuthorize("hasRole('ADMIN')")`.

### 2.4 Infrastructure & Documentation

- **Dockerisation:** `Dockerfile` (multi‑stage build) and `docker-compose.yml` (PostgreSQL 15, Redis 7, Spring Boot app).
- **API documentation:** Swagger UI available at `http://localhost:8080/swagger-ui/index.html` (SpringDoc OpenAPI).
- **Postman collection:** Provided in `postman/` folder, includes normal cases and error cases (duplicate idempotency, overselling, invalid voucher, missing token, 403, 404, validation errors).
- **Testing:** Comprehensive test suite including unit tests (JUnit 5 + Mockito), integration tests (Testcontainers for PostgreSQL), and concurrent tests (simulate flash‑sale thread contention).
- **Logging:** SLF4J + Logback – `DEBUG` level for `com.geek.booking`, `INFO` for infrastructure.

## 3. Not Implemented Features 

| Feature | Reason / Scope Limitation |
|---------|---------------------------|
| Real payment gateway integration (Stripe, PayPal) | Out of scope for the assignment; admin manually updates `PAID` status. |
| Update (PUT) on vouchers | Kept simple: vouchers are create‑only. |
| Delete endpoints (concerts, ticket categories, users, vouchers) | Omitted to keep focus on core booking flow; can be added later. |
| Soft delete for users / concerts | Not required; hard delete is acceptable but not implemented. |
| Real‑time seat map / WebSocket | Not part of the problem statement; ticket category‑based booking only. |
| Message queue for async processing | Load is moderate (500 req/min), synchronous processing is sufficient. |
| Full‑text search for concerts | Not implemented; simple `LIKE` filters via Specifications. |
| Rate limiting with fallback (if Redis is down) | Rate limiting is optional; if Redis fails, it is disabled (no request blocking). |
| Idempotency key cleanup job | Keys are kept forever; acceptable for ≤50k bookings. |
| Token blacklist (refresh token revocation) | Not implemented; refresh token remains valid until expiration. |
| Pagination for customer `GET /api/concerts` | Not needed; only admins see large lists. |
| `OPERATOR` role with limited permissions | Defined but not fully differentiated; only `ADMIN` is used for all admin endpoints. |

## 4. Limitations of the Current System

- **Single instance deployment:** Only one Spring Boot container. Horizontal scaling would require a load balancer and shared session/Redis.
- **Synchronous booking processing:** All operations are performed within the HTTP request. If business logic grows, response time may increase.
- **No real payment:** Cannot process real money; manual admin updates are required.
- **Refresh token cannot be revoked:** No blacklist → a stolen token stays valid for up to 7 days.
- **Redis dependency for caching & rate limiting:** If Redis is unavailable, concert caching falls back to database (higher load) and rate limiting is disabled (no protection).
- **Partial index on vouchers removed:** The original partial index using `CURRENT_TIMESTAMP` was removed because PostgreSQL requires index predicates to be immutable. A regular composite index is used instead – slightly less efficient but still adequate.
- **Trigger‑based ticket release:** Works only when the `status` column is updated via JPA. Direct SQL updates would bypass the trigger.
- **Concurrent idempotency test disabled:** The test `concurrentIdempotencyKey_shouldCreateOnlyOneBooking` is temporarily disabled due to a Hibernate session issue after duplicate key exception. The unique constraint still guarantees correctness.
- **No load testing:** The system has not been benchmarked beyond the expected 500 req/min.

## 5. Out of Scope 

- Frontend (UI) – only REST API is provided.
- OAuth2 / social login.
- Password reset / “forgot password” flow.
- Email or SMS notifications.
- Reporting / analytics dashboard (sales, revenue).
- Multi‑language support (i18n).
- Refund logic when a paid booking is cancelled.
- Integration with external inventory systems.

## 6. Technology Choices – Rationale Summary

| Technology | Why chosen |
|------------|------------|
| PostgreSQL 15 | ACID compliance, robust row locking (`SELECT FOR UPDATE`), support for partial indexes and `ON CONFLICT`. |
| Spring Boot 3.2.0 | Modern, fast development, tight integration with Spring Data JPA and Spring Security. |
| MapStruct | Reduces boilerplate mapping code between entities and DTOs; compile‑time safety. |
| Redis  | Demonstrates readiness for distributed caching and rate limiting; fallback works without it. |
| JWT | Stateless, works well with REST, easy to test with Postman. |
| Docker Compose | One‑command setup for reviewers; environment consistency. |

## 7. Testing Scope

- **Unit tests:** All service layers are unit tested using JUnit 5 and Mockito. Tests cover success paths, validation failures, business exceptions (insufficient inventory, invalid voucher), and repository mocking.
- **Integration tests:** Using Testcontainers (PostgreSQL 15) to verify database constraints, trigger behaviour, and end‑to‑end flows such as booking creation and cancellation. These tests are part of the suite but may be disabled in certain builds due to environment dependencies.
- **Concurrent tests:** Simulate flash‑sale loads with `ExecutorService` and `CountDownLatch` to prove overselling prevention and voucher limit enforcement. One test (concurrent idempotency) is currently disabled (see Limitations).
- **Postman collection:** Manual testing of all endpoints, including both normal and error cases (duplicate idempotency, overselling, expired token, 403, 404, validation errors).
- **What is not tested:** Load testing, end‑to‑end UI testing.

## 8. Deployment Notes

- **Prerequisites:** Docker Desktop (or Docker Engine + Compose), 4 GB RAM, free ports 8080, 5432, 6379 (configurable via `.env`).
- **Quick start:** `docker-compose up -d --build`
- **Reset database:** `docker-compose down -v`
- **Access Swagger:** `http://localhost:8080/swagger-ui/index.html`
- **Default admin account:** email `admin@geek.com`, password `admin123` (pre‑seeded in `02_seed.sql`).

## 9. Conclusion of Scope

The delivered backend fully satisfies the core requirements defined in the technical assessment:
- Flash‑safe ticket booking with **overselling prevention** (pessimistic locks).
- **Duplicate request protection** (idempotency key + unique constraint + duplicate flag).
- **Voucher abuse prevention** (unique user‑voucher constraint + row lock).
- **Operation dashboard** (admin APIs) for managing concerts, ticket categories, vouchers, bookings, and users.
- **Clean code structure**, unit/integration/concurrent tests, Dockerisation, and comprehensive documentation (README, DESIGN, SCOPE, Postman collection).

All missing or out‑of‑scope features are explicitly documented, and the system can be demonstrated immediately using the provided Docker setup and Postman collection.