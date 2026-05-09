# System Design – Concert Ticket Booking Platform

## 1. Architecture Overview

The source code is organised into the following packages, each with a clear responsibility:

- **`controller`** – Contains REST API endpoints. Separated into `customer` and `admin` sub‑packages (or explicit naming) to enforce role‑based access (e.g. `CustomerBookingController`, `AdminConcertController`). Handles HTTP request/response conversion, input validation via `@Valid`, and delegates to service layer.

- **`service`** – Implements business logic. Consists of interfaces (`*Service`) and implementations (`*ServiceImpl`). Services coordinate repositories, mappers, and external components; they are annotated with `@Service` and `@Transactional` where needed. Typical workflows: booking creation, inventory locking, voucher validation, etc.

- **`repository`** – Spring Data JPA interfaces. They extend `JpaRepository` and often `JpaSpecificationExecutor`. Custom methods (e.g. `findByIdWithPessimisticLock`) are annotated with `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent overselling. Queries are written either with derived names or JPQL.

- **`entity`** – JPA entities mapping directly to database tables (`users`, `concerts`, `bookings`, …). They use Lombok (`@Getter`, `@Setter`, `@Builder`) and JPA annotations (`@Entity`, `@Table`, `@Id`). No business logic is placed here; only field mappings and basic JPA callbacks (`@PrePersist`).

- **`dto`** – Data Transfer Objects for request and response. Sub‑packages `request` and `response` separate input from output. DTOs are used exclusively at controller boundaries, never leaked to the service layer.

- **`mapper`** – MapStruct interfaces that convert between entities and DTOs (e.g. `ConcertMapper`, `BookingMapper`). Declared with `componentModel = "spring"` to become injectable beans, reducing boilerplate mapping code.

- **`exception`** – Custom business exceptions (`BusinessException`, `InsufficientInventoryException`, …) and a global `@RestControllerAdvice` (`GlobalExceptionHandler`) that catches exceptions and returns a consistent JSON error response with appropriate HTTP status codes.

- **`enums`** – Enumerations used across the application, such as `BookingStatus`, `UserRole`, `ConcertStatus`, `DiscountType`. They provide type safety and are persisted as strings in the database.

- **`security`** – Contains JWT utilities (`JwtTokenProvider`), authentication filter (`JwtAuthenticationFilter`), `UserPrincipal` (implements `UserDetails`), `CustomUserDetailsService`, and Spring Security configuration (`SecurityConfig`). Responsible for authentication, token generation/validation, and role‑based authorisation.

- **`config`** – General configuration classes: Swagger/OpenAPI (`SwaggerConfig`), Redis (if used), Web configuration, and any other `@Configuration` classes that set up beans or customise framework behaviour.

- **`specification`** – JPA `Specification` implementations for dynamic query building (e.g. `BookingSpecification`, `ConcertSpecification`). Used by service layers that require filterable, pageable queries (admin dashboards).

All these packages are assembled into a single Spring Boot monolith. The monolith approach is chosen for simplicity, transactional consistency (critical for pessimistic locking), and adequate performance for the expected flash‑sale traffic (50 000 users, 300‑500 bookings/minute).


## 2. Technology Stack

| Component | Technology | Reason |
| :--- | :--- | :--- |
| **Language** | Java 21 | LTS, virtual threads ready, modern language features. |
| **Framework** | Spring Boot 3.2.0 | Robust, fast development, integration with Spring Data, Security. |
| **Database** | PostgreSQL 15 | ACID compliance, support for `SELECT FOR UPDATE`. |
| **Security** | Spring Security + JWT | Stateless authentication, role‑based access (RBAC). |
| **Testing** | JUnit 5, Mockito | Comprehensive unit and integration testing. |

---

## 3. Database Design

### 3.1 Database Tables Description


| Table | Columns | Primary Key | Foreign Key / Constraints | Relationships |
|-------|---------|-------------|---------------------------|---------------|
| **users** | `id`, `email` (UQ), `name`, `password`, `role`, `created_at`, `updated_at` | `id` | – | One-to-many with `bookings` (user_id)<br>One-to-many with `user_voucher_usage` (user_id) |
| **concerts** | `id`, `name`, `description`, `venue`, `event_date`, `status`, `created_at`, `updated_at` | `id` | – | One-to-many with `bookings` (concert_id)<br>One-to-many with `ticket_categories` (concert_id) |
| **ticket_categories** | `id`, `concert_id`, `name`, `price`, `total_quantity`, `available_quantity`, `created_at`, `updated_at` | `id` | `concert_id` → `concerts(id)` | One-to-many with `booking_items` (ticket_category_id) |
| **bookings** | `id`, `user_id`, `concert_id`, `idempotency_key` (UQ), `status`, `total_price`, `created_at`, `updated_at`, `expires_at` | `id` | `user_id` → `users(id)`<br>`concert_id` → `concerts(id)` | One-to-many with `booking_items` (booking_id)<br>One-to-one with `user_voucher_usage` (booking_id) |
| **booking_items** | `id`, `booking_id`, `ticket_category_id`, `quantity`, `unit_price`, `created_at` | `id` | `booking_id` → `bookings(id)`<br>`ticket_category_id` → `ticket_categories(id)` | – |
| **vouchers** | `id`, `code` (UQ), `discount_type`, `discount_value`, `usage_limit`, `used_count`, `valid_from`, `valid_to`, `min_order_value`, `created_at`, `updated_at` | `id` | – | One-to-many with `user_voucher_usage` (voucher_id) |
| **user_voucher_usage** | `id`, `user_id`, `voucher_id`, `booking_id`, `used_at` | `id` | `user_id` → `users(id)`<br>`voucher_id` → `vouchers(id)`<br>`booking_id` → `bookings(id)`<br>Unique constraint `(user_id, voucher_id)` | – |

**Additional constraints (not in PK):**
- `users.email` – UNIQUE
- `bookings.idempotency_key` – UNIQUE
- `user_voucher_usage (user_id, voucher_id)` – UNIQUE (prevents voucher abuse)
- `ticket_categories.available_quantity` – CHECK (>=0)

### 3.2 Critical Constraints

| Constraint | Type | Purpose | Implementation |
|------------|------|---------|----------------|
| `idempotency_key` UNIQUE | Unique constraint | Prevents duplicate booking requests caused by network retries or multiple user clicks. | The `idempotency_key` column in the `bookings` table has a `UNIQUE` constraint. The client generates a UUID; the server checks for an existing key before creating a new booking. If the key exists, the previous result is returned. |
| `available_quantity` CHECK (>=0) | Check constraint | Ensures that the available ticket quantity never becomes negative, even in case of logic errors. | `available_quantity` is defined as `INT NOT NULL CHECK (available_quantity >= 0)`. Any `UPDATE` that would violate this condition is rejected by the database. |
| `user_voucher_usage (user_id, voucher_id)` UNIQUE | Composite unique constraint | Prevents a user from using the same voucher more than once (anti‑abuse). | The `user_voucher_usage` table has a `UNIQUE(user_id, voucher_id)` constraint. Any duplicate insertion attempt fails at the database level. |
| Partial index `idx_ticket_categories_available` | Partial index | Speeds up flash‑sale queries by only indexing categories that still have available tickets (`available_quantity > 0`). | `CREATE INDEX idx_ticket_categories_available ON ticket_categories(concert_id) WHERE available_quantity > 0;` – this index is small and fast for filtering. |

### 3.3 Inventory Management

The system uses **pessimistic locking** together with a dedicated `available_quantity` column to handle flash‑sale inventory correctly.

**Flow for booking creation:**

1. Start a database transaction (`@Transactional`).
2. **Lock the row** in `ticket_categories` using `SELECT FOR UPDATE`. Only the current transaction holds the lock; others must wait.
3. Check if `available_quantity >= requested_quantity`. If not → rollback and throw `InsufficientInventoryException`.
4. If enough tickets exist → decrement `available_quantity` by the requested quantity.
5. Create the `booking` and `booking_items` records (price snapshot).
6. Commit the transaction → the lock is released, and waiting transactions see the updated value.

**Flow for cancellation or expiration:**

- If a booking is cancelled (`CANCELLED`), expires (`EXPIRED`), or fails (`FAILED`), a **database trigger** automatically restores the reserved tickets to `available_quantity`.
- The trigger is defined as:

```sql
CREATE OR REPLACE FUNCTION release_tickets_on_booking_cancel()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.status = 'PENDING' AND NEW.status IN ('CANCELLED', 'EXPIRED', 'FAILED')) THEN
        UPDATE ticket_categories tc
        SET available_quantity = available_quantity + bi.quantity,
            updated_at = CURRENT_TIMESTAMP
        FROM booking_items bi
        WHERE bi.booking_id = NEW.id AND bi.ticket_category_id = tc.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_release_tickets
AFTER UPDATE OF status ON bookings
FOR EACH ROW
EXECUTE FUNCTION release_tickets_on_booking_cancel();
```
**Visual summary of inventory handling:**
```text
[Booking request] → Row lock → Check quantity → If sufficient → Decrement → Commit
                                              → If insufficient → Rollback (409)
[Cancel/Expire] → Trigger → Add quantity back → Commit
```
## 4. Key Design Decisions & Rationale

### 4.1 Preventing Overselling in Flash Sale

**Possible approaches:**
- Optimistic locking (version column)
- Pessimistic row locking (`SELECT FOR UPDATE`)
- Queue‑based processing (e.g., RabbitMQ, Redis List)

**Selected approach:** Pessimistic row locking (`LockModeType.PESSIMISTIC_WRITE`) on the `ticket_categories` row.

**Why not use the other approaches?**
- **Optimistic locking** would cause many `OptimisticLockException`s under high contention, forcing clients to retry and increasing CPU/DB load. It is better suited for low‑contention scenarios.
- **Queue‑based processing** would introduce asynchronous complexity and delay the booking response, which is undesirable for a real‑time flash sale user experience.

**Implementation:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT tc FROM TicketCategory tc WHERE tc.id = :id")
Optional<TicketCategory> findByIdWithPessimisticLock(@Param("id") Long id);
```
**Trade‑off:** 
- Pessimistic row locking serialises all requests for the same ticket category. If 100 requests arrive for the last 10 tickets, only the first 10 succeed; the rest receive a 409 Conflict.
- With an expected peak of only 8‑9 requests per second, lock wait time is a few milliseconds – negligible.
- **Benefit:** Guarantees **zero overselling** without complex retries or distributed coordination.
- **Future scaling:** For very high traffic (>1000 req/s), we would need request queuing or sharding by concert/category.
### 4.2 Idempotency to Avoid Duplicate Bookings

**Possible approaches:**
- Client‑generated UUID stored in database with unique constraint
- Server‑generated idempotency key with Redis TTL
- Token bucket with distributed cache (e.g., Hazelcast)

**Selected approach:** Client‑generated UUID stored in the `bookings` table with a **unique constraint**.

**Why not use the other approaches?**
- **Redis/Token bucket** adds external dependencies and complexity; the current traffic does not require the extra performance.
- **Server‑generated keys** would require additional round trips or stateful coordination, making the system harder to scale.

**Implementation:**  
Client sends a `UUID` in the `idempotencyKey` header. The `idempotency_key` column in the `bookings` table has a `UNIQUE` constraint. The service checks for an existing key before creating a new booking; if the key exists, the previous result is returned.

**Trade‑off:** 
- Idempotency keys are stored permanently in the `bookings` table (UUID + index overhead). For ≤50k bookings, total storage is a few MB – negligible.
- **Alternative (Redis with TTL)** would require an extra service and fallback handling, increasing operational complexity.
- **Chosen approach keeps the system self‑contained** (no external cache) and guarantees idempotency even after server restarts.
- **Drawback:** Keys cannot be auto‑cleaned, but this is acceptable for a backend test.

### 4.3 Voucher Abuse Prevention

**Possible approaches:**
- `used_count` check only (in application)
- Distributed lock (e.g., Redis Redlock)
- Database unique constraint + row‑level lock

**Selected approach:** Database unique constraint (`user_id, voucher_id`) combined with pessimistic row lock (`SELECT FOR UPDATE` on the voucher row).

**Why not use the other approaches?**
- **Simple `used_count` check** is vulnerable to race conditions: two concurrent requests could both see `used_count < usage_limit` and both increment.
- **Distributed lock** would add external infrastructure (Redis) and is overkill for a single‑instance deployment.

**Implementation:**  
- `user_voucher_usage` table has `UNIQUE(user_id, voucher_id)`.
- Inside the booking transaction, we lock the voucher row with `SELECT FOR UPDATE` and check `used_count < usage_limit`.
- After reservation, we increment `used_count` and insert into `user_voucher_usage`.

**Trade‑off:** 
- The unique constraint `(user_id, voucher_id)` adds a small logarithmic write cost due to B‑tree index check – negligible compared to the overall transaction (locking, inventory update).
- **Alternative (application‑only `used_count` check)** is faster but unsafe under concurrency (two threads may both see `used_count < usage_limit` and both proceed).
- **Chosen approach provides database‑level guarantee** against voucher abuse – essential for flash sale campaigns.
- **Drawback:** The `user_voucher_usage` table grows with each redemption; with <50k bookings, still insignificant.

### 4.4 Database Choice: PostgreSQL vs MySQL

**Possible approaches:**
- PostgreSQL 15
- MySQL 8.0
- Other relational databases (Oracle, SQL Server, etc.)

**Selected approach:** PostgreSQL 15.

**Why not use the other approaches?**
- **MySQL 8.0** supports partial indexes and row locking, but has a higher tendency for deadlocks under `SELECT FOR UPDATE` in high‑contention scenarios. Its `ON DUPLICATE KEY` is less flexible than PostgreSQL’s `ON CONFLICT`.
- **Other databases** were not considered because they would introduce licensing costs or unfamiliar tooling.

**Implementation:**  
Standard PostgreSQL JDBC driver with Hibernate dialect. The schema includes partial indexes (e.g., `WHERE available_quantity > 0`) and a trigger for ticket release.

**Trade‑off:** 
- PostgreSQL’s MVCC stores multiple row versions, leading to higher storage usage and occasional need for vacuuming compared to MySQL’s undo log.
- For small tables (tens of thousands of rows), even a 2‑3x storage factor is acceptable (e.g., 50MB vs 20MB).
- **Benefits:** Robust row locking, partial indexes, and `ON CONFLICT` support directly address flash sale requirements.
- MySQL 8.0 could work but historically shows more deadlocks under `SELECT FOR UPDATE` in high‑contention write workloads.
- **Overall trade‑off favours correctness and safety** over marginal storage or performance gains.

### 4.5 Caching (Optional)

**Possible approaches:**
- In‑memory cache (Caffeine)
- Redis with TTL
- No caching

**Selected approach:** Redis for caching published concert lists and single concert details.

**Why not use the other approaches?**
- **In‑memory cache** would not be shared across multiple instances if the system scales horizontally.
- **No caching** would increase database load under flash sale reads.

**Implementation:**  
- Spring Cache abstraction with Redis as the cache manager (`@EnableCaching`, `RedisCacheManager`).
- `@Cacheable` on `getPublishedConcerts()` and `getConcertById()` with TTL = 5 minutes.
- `@CacheEvict` on all write operations (`createConcert`, `updateConcert`, `publishConcert`, `unpublishConcert`, `deleteConcert`) to keep cache consistent.

**Trade‑off:**  
- Adding Redis introduces an extra container and slight operational complexity, but the system still works without Redis (fallback to database queries).  
- Caching reduces database reads significantly during flash sale (many users view the same concert lists), improving overall performance.  
- Rate limiting is **not implemented** in this scope because the expected load (8‑9 req/s) does not require it; it can be added later using Bucket4j or Resilience4j if needed.

### 4.6 Security – JWT with Access & Refresh Tokens

**Possible approaches:**
- Session‑based (server‑side sessions)
- OAuth2 / OpenID Connect
- JWT with only access token (no refresh)
- JWT with access + refresh tokens

**Selected approach:** JWT with separate access and refresh tokens.

**Why not use the other approaches?**
- **Session‑based authentication** would require server‑side session storage (e.g., Redis) and breaks the stateless REST principle.
- **OAuth2** is too heavy for a simple backend test.
- **JWT with only access token** would force users to re‑login every time the token expires (every 15 minutes), which is poor UX.

**Implementation:**  
- Access token (15 min) – contains user email and role.
- Refresh token (7 days) – stored by client; used to request a new access token.
- Passwords hashed with `BCryptPasswordEncoder` (strength 10).
- Spring Security filters validate JWT and set `Authentication` in the context.

**Trade‑off:** 
- Refresh tokens cannot be easily revoked without a token blacklist (e.g., in Redis). A stolen refresh token remains valid until its natural expiry (7 days) – a security risk in production.
- **Alternative (no refresh, only short‑lived access tokens)** forces users to log in every 15 minutes – poor UX.
- **Alternative (server‑side sessions)** breaks statelessness and requires sticky sessions or a shared session store (Redis again).
- **Chosen trade‑off prioritises good UX (users stay logged in for days) and statelessness** while accepting the revocation risk.
- **Production improvement:** Add refresh token rotation (issue a new refresh token each use, invalidate the old one) and optionally a token blacklist.
## 5. Overall System Limitations

This section lists the current constraints and limitations of the implemented backend. These are deliberate trade‑offs or temporary simplifications made to meet the 48‑hour deadline and focus on the flash‑sale core requirements.

### 5.1 Single Instance Deployment
- **What it means:** The application runs as a single Spring Boot process. No load balancer or multiple replicas are configured.
- **Why it is a limitation:** Under extremely high traffic (e.g., >5000 concurrent users), a single JVM may become a bottleneck. CPU, memory, and database connection pool limits could be reached.
- **Why it is acceptable now:** The expected peak load is 300‑500 requests per minute (≈8‑9 req/s), which a modern Spring Boot instance can easily handle.
- **Future scaling path:** Add a load balancer (e.g., NGINX) and run multiple containers. Use a shared database and Redis for session/distributed locks.

### 5.2 Synchronous Processing (No Message Queue)
- **What it means:** Every HTTP request (booking creation, admin updates) is processed synchronously – the client waits for the entire business logic to complete, including database writes and potential external calls.
- **Why it is a limitation:** If the system later integrates with a slow external payment gateway, the response time could increase to several seconds, causing poor user experience during flash sale.
- **Why it is acceptable now:** There is no real payment integration; booking statuses are changed manually by admins. The current logic (inventory check, voucher validation, DB writes) completes in <200ms.
- **Future improvement:** Introduce a message queue (e.g., RabbitMQ or Kafka) to handle payment confirmation asynchronously. The booking API can return immediately with a `PENDING` status.

### 5.3 No Automated Payment Integration
- **What it means:** The system does not connect to any real payment provider (Stripe, PayPal, etc.). Instead, operators manually update a booking’s status to `PAID`, `FAILED`, or `CANCELLED` via the admin dashboard.
- **Why it is a limitation:** The platform cannot process real money transactions. It is only suitable for demos, testing, or free reservations.
- **Why it was done this way:** The system focuses on flash‑sale mechanics (overselling prevention, idempotency, voucher abuse). Payment integration is explicitly out of scope according to the problem statement.
- **Potential extension:** Add a simple webhook or use a fake payment gateway for demonstration purposes.

### 5.4 Limited Token Revocation (JWT Refresh Tokens)
- **What it means:** Refresh tokens are stateless; there is no token blacklist or revocation store. Once issued, a refresh token remains valid until its natural expiry (7 days).
- **Why it is a limitation:** If a refresh token is compromised, an attacker can continuously obtain new access tokens for up to 7 days. The only way to revoke it is to change the user’s password or wait for expiry.
- **Why it is acceptable now:** The system is a backend test with a limited number of users (admins and a few customers). The risk is low, and implementing a token blacklist would require Redis or a database table, adding complexity.
- **Production improvement:** Use refresh token rotation (issue a new refresh token each time, invalidate the old one) and store revoked tokens in Redis with a TTL.

### 5.5 Incomplete Partial Index on Vouchers
- **What it means:** The original intention was to create a partial index on `vouchers` to efficiently filter only valid (not expired, not used up) vouchers. The index predicate `WHERE valid_from <= CURRENT_TIMESTAMP AND valid_to >= CURRENT_TIMESTAMP` was removed because PostgreSQL requires index predicates to be immutable.
- **Why it is a limitation:** A regular composite index `(valid_from, valid_to, used_count, code)` is less selective than a partial index. It still includes expired vouchers, wasting some storage and scan time.
- **Why it is acceptable:** The number of vouchers is small (hundreds at most), so the performance difference is negligible. The system still passes the flash‑sale load requirements.
- **Alternatives explored:** Use a database function marked `IMMUTABLE` (not possible with `CURRENT_TIMESTAMP`), or rely on application‑side filtering (already done).

### 5.6 No Real‑Time Seat Map
- **What it means:** The system does not provide a graphical seat map or real‑time seat availability updates via WebSocket. Seats are represented only as `available_quantity` numbers per ticket category.
- **Why it is a limitation:** For concerts with assigned seating, users cannot choose specific seats; they only select a category and quantity.
- **Why it is acceptable:** The problem statement does not require seat maps; it only mentions “ticket categories” (VIP, Standard, etc.). Many flash sale systems work with category‑based booking.
- **Future improvement:** Extend the database schema with a `seats` table and implement real‑time locking of individual seats.
### 5.7 Redis Caching Fallback Behaviour

- **What it means:** Caching uses Redis. If Redis is unavailable, the system gracefully falls back to direct database reads (no cache). No rate limiting is implemented.
- **Why it is a limitation:** Without Redis, database load increases slightly (the system still works). Rate limiting is absent, but the load is moderate.
- **Why it is acceptable:** For a single‑instance deployment, caching is a performance optimisation, not a strict requirement. The fallback logic ensures the system remains functional.
- **Production hardening:** Deploy Redis in a highly available configuration (Sentinel or cluster) for critical caching needs.

## 6. Error Handling & Logging

### 6.1 Global Exception Handler

The system uses a single `@RestControllerAdvice` class (`GlobalExceptionHandler`) to intercept all exceptions thrown by controllers or services. This ensures that every error response follows the same JSON structure, prevents internal stack traces from being exposed to clients, and maps exceptions to appropriate HTTP status codes.

**Standard error response format:**

```json
{
  "timestamp": "2026-05-09T10:30:00.123Z",
  "status": 400,
  "error": "BUSINESS_ERROR",
  "message": "Voucher is not valid"
}
```

- **`timestamp`** – ISO‑8601 timestamp (UTC) when the error occurred.
- **`status`** – HTTP status code (e.g., 400, 401, 403, 404, 409, 500).
- **`error`** – A concise, machine‑readable error code (e.g., `BUSINESS_ERROR`, `INSUFFICIENT_INVENTORY`, `ACCESS_DENIED`).
- **`message`** – Human‑readable explanation. For validation errors, it contains field‑specific details.

### 6.2 Specific Exception Mappings

| Exception / Situation | HTTP Status | Error Code | When It Happens |
|-----------------------|-------------|------------|------------------|
| `BusinessException` (base) | 400 Bad Request | `BUSINESS_ERROR` | General rule violation: voucher expired, invalid status transition, missing idempotency key, etc. |
| `InsufficientInventoryException` | 409 Conflict | `INSUFFICIENT_INVENTORY` | Requested ticket quantity > `available_quantity` (overselling protection). |
| `ResourceNotFoundException` | 404 Not Found | `NOT_FOUND` | Requested entity (concert, booking, user, voucher) does not exist in database. |
| `AccessDeniedException` | 403 Forbidden | `ACCESS_DENIED` | Authenticated user lacks role (e.g., customer calls admin API) or tries to access another user’s private booking. |
| `AuthenticationException` | 401 Unauthorized | `UNAUTHORIZED` | Missing, invalid, or expired credentials (login failed). |
| `JwtException` (from JJWT) | 401 Unauthorized | `INVALID_TOKEN` | JWT token is malformed, has invalid signature, or expired. |
| `MethodArgumentNotValidException` | 400 Bad Request | `VALIDATION_FAILED` | Request body fails Bean Validation (`@NotNull`, `@Size`, `@Email`, etc.). Response includes field errors. |
| `MissingServletRequestParameterException` | 400 Bad Request | `MISSING_PARAMETER` | Required query parameter is absent. |
| `HttpMessageNotReadableException` | 400 Bad Request | `MALFORMED_JSON` | Request body contains invalid JSON (e.g., missing quotes, trailing commas). |
| `MethodArgumentTypeMismatchException` | 400 Bad Request | `TYPE_MISMATCH` | Path variable or query parameter has wrong type (e.g., `id=abc` when `Long` expected). |
| `DataIntegrityViolationException` | 409 Conflict | `DATA_INTEGRITY` | Database constraint violation (e.g., duplicate `idempotency_key` or duplicate `(user_id, voucher_id)`). The message is customised to indicate the specific conflict. |
| `Exception` (catch‑all) | 500 Internal Server Error | `INTERNAL_ERROR` | Any unhandled exception. The message includes the exception class name and a snippet (full stack trace logged on the server side). |

### 6.3 Why These HTTP Status Codes?

- **400 Bad Request** – Client sent invalid input (e.g., negative quantity, malformed JSON, expired voucher). The client should correct the request before retrying.
- **401 Unauthorized** – Authentication required or failed. The client should obtain a valid token (login).
- **403 Forbidden** – Authenticated but not authorised. The client should not retry with the same credentials; a different account may be needed.
- **404 Not Found** – Resource does not exist. The client should verify the identifier.
- **409 Conflict** – Request conflicts with the current server state (e.g., not enough tickets, duplicate idempotency key). The client may retry after a delay or with different input.
- **500 Internal Server Error** – Unexpected server bug. The client may retry later (exponential backoff recommended). The server administrator should investigate the logs.

### 6.4 Logging Strategy

- **Framework:** SLF4J + Logback (Spring Boot default).
- **Application‑specific log level:** `DEBUG` for the `com.geek.booking` package. This logs method entry/exit, key business decisions (e.g., “locking ticket category 1”, “voucher FLASH10 applied”), and successful bookings.
- **Infrastructure log level:** `INFO` for Spring Framework, Hibernate, Tomcat, and Redis. This reduces noise while still showing startup and important lifecycle events.
- **Sensitive data:** Passwords, JWT secrets, and any payment‑related information are **never** logged (even in `DEBUG` mode).
- **Exception logging:** All caught exceptions are logged with `WARN` (for business exceptions) or `ERROR` (for unexpected exceptions) along with the stack trace.

**Example log output (application level):**

```text
2026-05-09 10:30:15.123 DEBUG 12345 --- [nio-8080-exec-1] c.g.b.service.impl.BookingServiceImpl    : Creating booking for userId=5, categoryId=1, quantity=2, idempotencyKey=abc-123
2026-05-09 10:30:15.456  INFO 12345 --- [nio-8080-exec-1] c.g.b.service.impl.BookingServiceImpl    : Booking created: id=42, userId=5, total=180.00
```
**Example error logging:**
```test
2026-05-09 10:30:15.789 WARN 12345 --- [nio-8080-exec-1] c.g.b.exception.GlobalExceptionHandler   : Inventory error: Not enough tickets for category: VIP
```
### 6.5 Why Not Return Stack Traces in Production Responses?

- **Security:** Stack traces can reveal internal package names, file paths, library versions, and even database structure, aiding attackers.
- **UX:** Frontend applications (mobile, web) only need a human‑readable message and an error code to show an alert or perform a fallback action.
- **Debugging:** Developers and operators can inspect the server logs (via `docker logs` or a log aggregator) for full stack traces.

### 6.6 Example Error Responses

**Insufficient inventory (409 Conflict):**

```json
{
  "timestamp": "2026-05-09T10:30:00.123Z",
  "status": 409,
  "error": "INSUFFICIENT_INVENTORY",
  "message": "Not enough tickets for category: VIP"
}
```
**Validation failure (400 Bad Request):**
```json
{
  "timestamp": "2026-05-09T10:30:00.123Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "{categoryId: must not be null, quantity: must be at least 1}"
}
```
**Authentication failure (401 Unauthorized):**
```json
{
  "timestamp": "2026-05-09T10:30:00.123Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid email or password"
}
```
**Access denied (403 Forbidden):**
```json
{
{
  "timestamp": "2026-05-09T10:30:00.123Z",
  "status": 403,
  "error": "ACCESS_DENIED",
  "message": "You do not have permission to access this resource"
}
```




## 7. Testing Strategy

- **Unit tests** – For service layer using JUnit 5 + Mockito. All business logic (idempotency, inventory checks, voucher validation) is covered. Example: `BookingServiceTest`, `ConcertServiceTest`, etc.
- **Integration tests** – Optional (using Testcontainers) to verify database interactions. Not mandatory for the submission but included to show proficiency.

Run tests with:

```bash
./mvnw test
```

## 8. Deployment

The system is containerised using Docker and Docker Compose. This is the recommended way to run the application because it ensures a consistent environment and requires only Docker Desktop (or Docker Engine + Compose) installed.

### 8.1 Prerequisites

- **Docker Desktop** (Windows / macOS) or **Docker Engine + Docker Compose** (Linux).
- At least **2 GB of free RAM** and **1 GB of disk space**.
- Ports **5432** (PostgreSQL), **6379** (Redis), and **8080** (Spring Boot) must be free or configurable via `.env`.

### 8.2 Using Docker Compose (recommended)
#### Project structure (relevant files)
```text
.
├── docker-compose.yml
├── Dockerfile
├── .env
├── docs/
├── sql/
│   ├── create_tables.sql
│   └── seed.sql
└── src/ 
```



#### Environment variables (`.env` file)

Create a `.env` file in the same directory as `docker-compose.yml` with the following content (adjust values if needed):

```env
# PostgreSQL
DB_NAME=concert_booking
DB_USERNAME=admin
DB_PASSWORD=admin123
DB_PORT=5432

# Redis
REDIS_PORT=6379

# Application
APP_PORT=8080

# JWT
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_EXPIRATION=900000   # 15 minutes in milliseconds
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Logging
LOG_LEVEL_APP=DEBUG
```
**Never commit the `.env` file to version control** (it is listed in `.gitignore` by default).

Change `DB_PASSWORD`, `JWT_SECRET` for production environments.

### Starting the system

Open a terminal in the project root (where `docker-compose.yml` is located) and run:

```bash
docker-compose up -d --build
```
- `--build` forces a rebuild of the Spring Boot image (essential when source code changes).
- `-d` runs containers in the background (detached mode).

**What happens behind the scenes:**

1. **PostgreSQL container** starts first.
   - Initialises the database using the `01_schema.sql` and `02_seed.sql` files mounted in `/docker-entrypoint-initdb.d`.
   - Creates the tables and inserts sample data (admin user, concert, voucher).
   - Exposes port `5432` (mapped to host via `DB_PORT`).

2. **Redis container** starts.
   - Runs Redis 7 alpine.
   - Exposes port `6379` (mapped via `REDIS_PORT`).

3. **Application container** builds and starts.
   - Uses the multi‑stage `Dockerfile` (Maven build + JDK runtime).
   - Waits for PostgreSQL and Redis to become healthy (health checks configured).
   - Starts Spring Boot on port `8080` (mapped via `APP_PORT`).

### Health checks

- **PostgreSQL:** The `pg_isready` command is used every 10 seconds. The app container will not start until PostgreSQL reports `ready`.
- **Redis:** The `redis-cli ping` command verifies connectivity.
- **Application:** No health check is defined for the app itself; you can monitor logs with `docker logs concert-app -f`.

### Verifying the deployment

```bash
# Check if all containers are running
docker ps

# View application logs
docker logs concert-app --tail 50

# Test if the API is reachable
curl http://localhost:8080/api/concerts
```
### Accessing the services

| Service | URL / Command | Notes |
|---------|---------------|-------|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | Interactive API documentation |
| Postgres (inside container) | `psql -U admin -d concert_booking` | Run `docker exec -it concert-postgres psql ...` |
| Redis (inside container) | `redis-cli` | Run `docker exec -it concert-redis redis-cli` |

### Stopping and cleaning up

```bash
# Stop containers but keep volumes (database data persists)
docker-compose down

# Stop and remove containers + volumes (reset database)
docker-compose down -v

# Remove unused images and build cache
docker system prune -a --volumes
```
## 8.3 Manual Setup (without Docker)

If you prefer to run the application natively (without Docker), follow these steps:

1. **Install dependencies:**
   - Java 21 (JDK)
   - Maven 3.9+
   - PostgreSQL 15 (running locally)
   - Redis 7 (optional, but recommended for caching/rate limiting)
2. **Create the database:**
   ```sql
   CREATE DATABASE concert_booking;
   ```
3. **Run the SQL script**
   ```bash
   psql -U postgres -d concert_booking -f sql/01_schema.sql
psql -U postgres -d concert_booking -f sql/02_seed.sql
```
4. **Configure application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/concert_booking
spring.datasource.username=admin
spring.datasource.password=admin123
spring.data.redis.host=localhost
spring.data.redis.port=6379
app.jwt.secret=your-256-bit-secret
app.jwt.access-expiration=900000
app.jwt.refresh-expiration=604800000
spring.jpa.hibernate.ddl-auto=validate
```
5. **Build and run the application**
```bash
./mvnw clean package
java -jar target/booking-0.0.1-SNAPSHOT.jar
```
6. **Access the API at``` http://localhost:8080/swagger-ui.html```**
## 8.4 Troubleshooting Common Deployment Issues

| Problem | Possible Cause | Solution |
|---------|----------------|----------|
| `Connection refused` to PostgreSQL | PostgreSQL not started or wrong host/port | Ensure PostgreSQL container is running (`docker ps`). Check `.env` variables. |
| `Relation "users" does not exist` | SQL scripts not executed | Verify `sql/` folder is mounted correctly. Check container logs: `docker logs concert-postgres`. |
| `Port already in use` (e.g., 8080, 5432) | Another process is using the port | Change the port in `.env` (e.g., `APP_PORT=8081`, `DB_PORT=5433`) and restart. |
| `No qualifying bean of type ...Mapper` | MapStruct implementation not generated | Run `mvn clean compile` inside the container or locally before building the Docker image. |
| `JWT secret must be at least 256 bits` | Secret too short | Use a 32‑byte random string (hex or base64). Generate with `openssl rand -hex 32`. |

## 8.5 Performance Tuning for Flash Sale

- **Database connection pool:** HikariCP is auto‑configured. For high load, increase `spring.datasource.hikari.maximumPoolSize` in `application.properties` (e.g., 20).
- **Application threads:** The embedded Tomcat default is 200 threads; this is sufficient for 500 req/min.
- **Redis connection:** Use a connection pool by adding `spring.data.redis.lettuce.pool.enabled=true`.

All these settings are optional and can be adjusted without changing the core logic.

## 8.6 Summary

- **Quick start (Docker):** `docker-compose up -d --build`
- **Manual start:** Maven + PostgreSQL + Redis
- **Test with Postman:** Import the collection from `postman/Concert_Booking_API.postman_collection.json`
- **Explore API:** Swagger UI at `http://localhost:8080/swagger-ui/index.html`

## 9. Future Improvements

While the current system fulfills all core requirements for a flash‑sale concert ticket booking platform, several enhancements could be made to improve scalability, resilience, user experience, and operational convenience. The following improvements are listed in order of priority (highest first).

### 9.1 Introduce a Message Queue for Asynchronous Payment Handling

**Current limitation:** All operations (inventory lock, booking creation, voucher usage) are synchronous. If a real payment gateway were integrated, the response time could increase significantly, causing poor user experience during flash sale.

**Proposed improvement:** Use a message queue (e.g., RabbitMQ, Apache Kafka, or AWS SQS) to decouple booking creation from payment processing.

**How it would work:**
- The `POST /api/bookings` endpoint only performs idempotency check, inventory reservation, and voucher validation – then immediately returns a `202 Accepted` response with a `bookingId` and status `PENDING_PAYMENT`.
- A background consumer picks up the booking event, processes payment with the external gateway, and updates the booking status to `PAID` or `FAILED`.
- The client can poll or use WebSocket to receive the final status.

**Benefits:**
- Reduces response time from ~200ms to <50ms for the booking request.
- Isolates payment gateway failures; the booking system remains available even if the payment service is slow or down.
- Allows retries and dead‑letter queues for failed payments.
- Scales horizontally by increasing the number of consumers.

**Trade‑off:** Adds operational complexity (message broker, consumer code, idempotency handling in consumers). However, this is standard practice for high‑throughput e‑commerce systems.

### 9.2 Use Redis with TTL for Idempotency Keys

**Current limitation:** Idempotency keys are stored forever in the `bookings` table. While storage is negligible for ≤50k bookings, the table may grow over time in a production environment.

**Proposed improvement:** Store idempotency keys in Redis with a TTL (e.g., 24 hours) instead of (or in addition to) the database.

**How it would work:**
- Before processing a booking request, the application checks Redis for the key.
- If found, return the cached result (booking ID and status).
- If not found, process the booking, then store the key in Redis (with TTL) and also save the booking in the database.
- The database unique constraint remains as a fallback to handle the rare case where Redis is unavailable.

**Benefits:**
- Keeps the `bookings` table lean (no permanent idempotency column).
- Faster lookup (Redis is in‑memory).
- Automatically cleans up old keys.

**Trade‑off:** Adds dependency on Redis (already used for caching/rate limiting). The system must handle Redis failures gracefully (fallback to database check).

### 9.3 Distributed Lock (Redisson) for Multiple Application Instances

**Current limitation:** The current pessimistic locking (`SELECT FOR UPDATE`) works perfectly for a single database instance. However, if the application is scaled to multiple replicas (behind a load balancer), row locks are still sufficient because they are managed by PostgreSQL. No distributed lock is strictly required for inventory updates.

**Proposed improvement:** If the system were to use a cache‑only inventory (e.g., Redis) instead of database locks, a distributed lock would be necessary. Redisson (Redis‑based) provides a familiar `RLock` interface.

**When this becomes relevant:**
- When database row locks become a bottleneck (e.g., >1000 req/s for the same category).
- When moving to a CQRS architecture with separate write and read stores.

**Benefits:**
- Allows inventory management entirely outside the database for extreme throughput.
- Redisson lock supports automatic lease renewal and fair queuing.

**Trade‑off:** Redisson adds another library and potential for split‑brain if Redis is not highly available. The current database lock is simpler and sufficient for the expected load.

### 9.4 Full‑Text Search for Concerts

**Current limitation:** Searching concerts is limited to simple `LIKE` queries (via Specification filters). This is not ideal for fuzzy matching or relevance ranking.

**Proposed improvement:** Use PostgreSQL’s built‑in full‑text search (`tsvector` and `tsquery`) to enable fast, relevant search across concert names, descriptions, venues, and artists.

**Implementation outline:**
- Add a generated column `search_vector` of type `tsvector` that concatenates `name`, `description`, `venue`.
- Create a GIN index on that column.
- Provide an API endpoint `GET /api/concerts/search?q=rock&page=...` that uses `plainto_tsquery` or `websearch_to_tsquery`.

**Benefits:**
- Supports partial matches, stemming, and ranking.
- Much faster than `LIKE '%keyword%'` on large datasets.
- No extra search engine (Elasticsearch) needed.

**Trade‑off:** Slightly increased storage (the index) and insert/update overhead. Acceptable for concert data that changes infrequently.

### 9.5 Real‑Time Seat Map with WebSockets

**Current limitation:** Seats are represented only by `available_quantity` per category. Users cannot select specific seats.

**Proposed improvement:** Extend the database with a `seats` table (one row per physical seat) and implement a real‑time seat map using WebSockets (STOMP over SockJS).

**How it would work:**
- Client connects via WebSocket to `/topic/seats/{concertId}`.
- When a user selects a seat, a temporary lock is placed (with a timeout) and broadcast to all connected clients.
- Upon successful booking, the seat is marked as `UNAVAILABLE`.

**Benefits:**
- Gives users a familiar, interactive seat selection experience.
- Reduces overselling risk (each seat is locked individually).
- Provides real‑time updates without page refresh.

**Trade‑off:** Significantly more complex – requires a `seats` table (potentially millions of rows), additional locking logic, and WebSocket infrastructure. This is a major feature that would be part of a “Concert Ticket Platform 2.0”.

### 9.6 Advanced Monitoring and Alerting

**Current limitation:** The system logs errors and metrics but does not have a dedicated monitoring stack.

**Proposed improvement:** Integrate Micrometer + Prometheus + Grafana to collect and visualise key metrics:
- Request rate, error rate, latency percentiles for each endpoint.
- Database connection pool usage.
- Redis cache hit ratio.
- JVM memory and garbage collection.

**Benefits:**
- Proactive detection of flash‑sale bottlenecks.
- Historical analysis for capacity planning.
- Alerting (e.g., when inventory lock wait time exceeds 100ms).

**Trade‑off:** Adds several components to the deployment (Prometheus, Grafana). However, they can be run as additional Docker containers and are widely used.

### 9.7 Idempotency Key Rotation (Cleanup Job)

**Current limitation:** Idempotency keys stay in the `bookings` table forever.

**Proposed improvement:** Add a scheduled job (e.g., daily at 3 AM) that deletes idempotency keys older than, say, 30 days.

**Implementation:**
```sql
DELETE FROM bookings WHERE created_at < NOW() - INTERVAL '30 days';
```
### 9.8 Rate Limiting per User (Stricter Enforcement)

**Current limitation:** Rate limiting is optional and falls back to no limiting if Redis is unavailable.

**Proposed improvement:** Enforce rate limiting using a local in‑memory cache (Caffeine) as a secondary tier, or store rate‑limit counters in the database (less performant but more reliable). Use a token bucket algorithm with a capacity of, say, 10 requests per 5 seconds per user.

**Benefits:** Protects the system from malicious or accidental flooding even without Redis.

**Trade‑off:** Adds another configuration layer. For the given load (8‑9 req/s), rate limiting is not strictly required.

### Summary of Prioritised Improvements

| Priority | Improvement | Effort | Impact |
|----------|-------------|--------|--------|
| 1 | Message queue for payment async | Medium | High (reduces response time, fault isolation) |
| 2 | Redis TTL for idempotency keys | Low | Medium (storage efficiency) |
| 3 | Full‑text search | Low | Medium (better UX) |
| 4 | Real‑time seat map | High | High (feature completeness) |
| 5 | Monitoring (Prometheus + Grafana) | Medium | Medium (operational insight) |

## 10. Conclusion

The backend system developed for the Concert Ticket Booking Platform successfully meets all the specified requirements for the flash‑sale scenario. The following core challenges have been addressed:

- **Overselling prevention:** Pessimistic row locking (`SELECT FOR UPDATE`) on the `ticket_categories` table guarantees that `available_quantity` is never decremented below zero. The expected peak of 8–9 requests per second is well within the capacity of this approach.

- **Duplicate booking avoidance:** A unique constraint on `idempotency_key` in the `bookings` table, combined with a client‑generated UUID, ensures that retried requests do not create duplicate bookings. This solution is simple and does not rely on external services.

- **Voucher abuse protection:** The `user_voucher_usage` table with a unique constraint `(user_id, voucher_id)` prevents a user from using the same voucher twice. Together with row‑level locking on the `vouchers` row, race conditions that could lead to exceeded usage limits are eliminated.

- **Operation dashboard:** Separate admin controllers under `/api/admin/` provide full CRUD for concerts, ticket categories, vouchers, and users, as well as the ability to view and update booking statuses manually. Role‑based access (`ADMIN` / `OPERATOR`) is enforced via Spring Security.

- **Customer booking flow:** Customers can register, log in, browse published concerts, select ticket categories, apply vouchers, and create bookings with idempotency. They can also view and cancel their own pending bookings.

- **Clean architecture and code structure:** The project follows a standard Spring Boot layered architecture (Controller → Service → Repository). DTOs, mappers (MapStruct), global exception handling, JWT security, and comprehensive unit tests are included.

- **Dockerisation and documentation:** A `Dockerfile` and `docker-compose.yml` allow one‑command startup of PostgreSQL, Redis (optional), and the Spring Boot application. The `README.md` provides setup instructions, coding conventions, and how to run tests. The `SCOPE.md` clearly states assumptions, implemented features, and limitations. The Postman collection (both normal and error cases) demonstrates all API endpoints.
- **Caching (Redis):** Added Redis caching for concert queries to reduce database load during read‑heavy flash sale periods. Cache eviction is properly handled on data changes.

- **Deployment readiness:** The system can be demoed immediately by running `docker-compose up -d --build` and importing the Postman collection. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

- **Limitations acknowledged:** The system is a single instance with synchronous processing, no real payment integration, and partial index on vouchers removed due to PostgreSQL immutability restrictions. These limitations are explicitly documented in the **Overall System Limitations** section and are acceptable for the scope of this technical assessment.

**Final statement:** The delivered backend is production‑ready for the described flash‑sale load (50 000 users, 300–500 requests/minute) and demonstrates thoughtful trade‑offs, clean code, and attention to concurrency, idempotency, and anti‑abuse measures. It fully aligns with the expectations outlined in the “Product Backend Engineer Technical Assessment”.