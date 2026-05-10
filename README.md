# Concert Ticket Booking Platform

A backend system for a concert ticket booking platform, designed to handle flash‑sale traffic (50 000 users, 300‑500 requests per minute).  
It prevents overselling, duplicate bookings, and voucher abuse, while providing a full admin dashboard.

---

## Table of Contents
- [Concert Ticket Booking Platform](#concert-ticket-booking-platform)
  - [Table of Contents](#table-of-contents)
  - [Technology Stack](#technology-stack)
  - [Prerequisites](#prerequisites)
  - [Quick Start with Docker (Recommended)](#quick-start-with-docker-recommended)
    - [Environment Variables (`.env`)](#environment-variables-env)
  - [Accessing the Services](#accessing-the-services)
  - [Stopping \& Cleaning Up](#stopping--cleaning-up)
  - [Manual Setup (Without Docker)](#manual-setup-without-docker)
  - [Testing](#testing)
  - [API Documentation \& Postman](#api-documentation--postman)
    - [Swagger UI](#swagger-ui)
    - [Postman Collection](#postman-collection)
  - [Project Structure](#project-structure)
  - [Limitations \& Known Issues](#limitations--known-issues)
  - [Future Improvements](#future-improvements)
  - [License \& Contact](#license--contact)

---

## Technology Stack

| Component       | Technology                                 | Reason                                                                 |
|----------------|--------------------------------------------|------------------------------------------------------------------------|
| Language       | Java 21                                    | LTS, virtual threads ready, modern features.                           |
| Framework      | Spring Boot 3.2.0                          | Robust, fast development, integration with Spring Data & Security.     |
| Database       | PostgreSQL 15                              | ACID compliance, support for `SELECT FOR UPDATE` (pessimistic locks).  |
| Caching        | Redis 7 (optional)                         | Caches concert lists; falls back to database if Redis is unavailable.  |
| Security       | Spring Security + JWT                      | Stateless authentication, role‑based access (RBAC).                    |
| Testing        | JUnit 5, Mockito, Testcontainers           | Unit, integration, and concurrent tests.                               |
| Containerisation | Docker & Docker Compose                   | One‑command environment setup.                                         |

---

## Prerequisites

- **Docker Desktop** (Windows / macOS) or **Docker Engine + Docker Compose** (Linux)
- **4 GB of free RAM** and **1 GB disk space**
- Ports **8080** (app), **5432** (PostgreSQL), **6379** (Redis) must be free or configurable via `.env`
- (Optional) **JDK 21** and **Maven 3.9+** for manual build

---

## Quick Start with Docker (Recommended)

Clone the repository and navigate into the project folder.

### Environment Variables (`.env`)

Create a `.env` file in the project root (same directory as `docker-compose.yml`) with the following content.  
Adjust values if needed, but keep the defaults for local testing.

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

# JWT (use a 32‑byte hex secret in production)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_EXPIRATION=900000      # 15 minutes (milliseconds)
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Logging
LOG_LEVEL_APP=DEBUG
Never commit the .env file to version control (it is already ignored by .gitignore).

## Running the System

Open a terminal in the project root and run:

```bash
docker-compose up -d --build
```
`--build` forces a rebuild of the Spring Boot image (essential after code changes).  
`-d` runs containers in the background.

**What happens behind the scenes:**

- PostgreSQL container starts and initialises the database using `sql/create_tables.sql` and `sql/seed.sql`.
- Redis container starts (used for caching).
- Application container builds and starts (waits for PostgreSQL and Redis to become healthy).

## Accessing the Services

| Service         | URL / Command                                               |
|-----------------|-------------------------------------------------------------|
| Swagger UI      | `http://localhost:8080/swagger-ui/index.html`               |
| PostgreSQL      | `docker exec -it concert-postgres psql -U admin -d concert_booking` |
| Redis           | `docker exec -it concert-redis redis-cli`                   |
| Application logs| `docker logs concert-app -f`                                |

## Stopping & Cleaning Up

```bash
# Stop containers (keep database volume)
docker-compose down

# Stop + remove containers & volumes (reset database)
docker-compose down -v

# Remove unused images and build cache
docker system prune -a --volumes
```
## Manual Setup (Without Docker)

If you prefer to run the application natively, follow these steps:

1. **Install dependencies**: JDK 21, Maven 3.9+, PostgreSQL 15, Redis 7 (optional).

2. **Create the database**:

   ```sql
   CREATE DATABASE concert_booking;
   ```
   
3. **Run the SQL scripts**:
   ```bash
   psql -U postgres -d concert_booking -f sql/create_tables.sql
   psql -U postgres -d concert_booking -f sql/seed.sql
```
4. **Configure `application.properties` (or use environment variables)**:
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
5. **Build and run**:
   ```
   ./mvnw clean package
java -jar target/booking-0.0.1-SNAPSHOT.jar
```
6. **Access Swagger UI at `http://localhost:8080/swagger-ui/index.html.`**:
## Testing

The project includes **unit**, **integration**, and **concurrent** tests.

- **Unit tests** (service layer) use JUnit 5 and Mockito – they do not require a database.
- **Integration tests** use Testcontainers to spin up a temporary PostgreSQL container.  
  They verify database constraints, triggers, and end‑to‑end flows.
- **Concurrent tests** simulate flash‑sale thread contention (`ExecutorService` + `CountDownLatch`) to prove overselling prevention and voucher limit enforcement.

> **Note:** Integration and concurrent tests **require Docker** to be running (Testcontainers starts containers automatically).  
> If you do not have Docker, you can disable them by adding `@Disabled` on the test classes.

**Run all tests:**

```bash
./mvnw clean test
```
## API Documentation & Postman

### Swagger UI

Once the application is running, interactive API documentation is available at:
`http://localhost:8080/swagger-ui/index.html`

You can explore all endpoints, see request/response schemas, and even execute requests directly (remember to include the JWT token for protected endpoints).

### Postman Collection

A complete Postman collection is provided in the repository:  
`booking_system.postman_collection.json`

**How to use it:**

1. **Import the collection**  
   - Open Postman.  
   - Click **Import** → **Upload Files** → select `booking_system.postman_collection.json`.  
   - The collection “Concert Booking Platform API” will appear.

2. **Set up environment variables**  
   The collection uses variables like `{{base_url}}`, `{{adminToken}}`, `{{customerToken}}`.  
   - `base_url` is preset to `http://localhost:8080`.  
   - The other tokens are automatically stored when you run the login requests.

3. **Login first**  
   - Open the `Normal Cases → Authentication` folder.  
   - Execute **“Login as Admin”** – this will store `adminToken` and `adminRefreshToken`.  
   - Execute **“Login as Customer”** – this will store `customerToken`.  
   > **All subsequent requests that require authentication will use these tokens automatically**.

4. **Explore the requests**  
   The collection is organised into folders:  
   - `Customer - Public` (no auth needed)  
   - `Customer - Booking` (requires customer token)  
   - `Admin - Concert / Ticket Category / Voucher / Booking / User Management` (require admin token)  
   - `Error Cases` (validation, business logic, authentication, not found, idempotency)

5. **Important** – for booking creation requests, the `idempotencyKey` field **must be a unique UUID each time**.  
   The pre‑request script automatically generates a new UUID when you run the request, so you don’t need to change it manually.  
   If you copy the request body, replace the key with `{{$uuid}}` or a new value.

---

## Project Structure

```text
├── docker-compose.yml
├── Dockerfile
├── .env
├── pom.xml
├── README.md
├── docs/
│   ├── Design.md          # Architecture, decisions, trade‑offs
│   └── Scope.md           # Assumptions, implemented features, limitations
├── sql/
│   ├── create_tables.sql  # Schema + triggers
│   └── seed.sql           # Initial data (admin, customer, concert, voucher)
├── src/
│   ├── main/
│   │   ├── java/com/geek/booking/
│   │   │   ├── config/        # Swagger, Security, Cache, etc.
│   │   │   ├── controller/    # REST endpoints (customer, admin)
│   │   │   ├── service/       # Business logic (with interfaces & impls)
│   │   │   ├── repository/    # Spring Data JPA + custom queries with @Lock
│   │   │   ├── entity/        # JPA entities (Lombok)
│   │   │   ├── dto/           # Request / response DTOs
│   │   │   ├── mapper/        # Manual mappers (no MapStruct to avoid annotation issues)
│   │   │   ├── exception/     # Custom exceptions + global handler
│   │   │   ├── enums/         # BookingStatus, ConcertStatus, UserRole, DiscountType
│   │   │   ├── security/      # JWT provider, filter, UserDetails
│   │   │   └── specification/ # Dynamic queries for admin filters
│   │   └── resources/
│   │       └── application.properties
│   └── test/                  # Unit, integration, and concurrent tests
└── booking_system.postman_collection.json
```
## Limitations & Known Issues

- **Single instance deployment** – horizontal scaling would require a load balancer and shared Redis.
- **Synchronous processing** – all operations are in‑request; future payment integration would need a message queue.
- **No real payment gateway** – `PAID` status is updated manually by admins.
- **Refresh tokens cannot be revoked** – no token blacklist (acceptable for assessment).
- **Redis caching** – caches concert details including `availableQuantity`, which changes often.  
  The cache is evicted on every booking/cancellation, but a better design would cache only static concert info.
- **Idempotency concurrent test** – currently disabled due to Hibernate session limitations; the unique constraint still guarantees correctness.
- **No scheduled job for expired bookings** – bookings may stay `PENDING` after `expires_at` (can be added with `@Scheduled`).

For a complete list, see [`docs/Scope.md`](./docs/Scope.md).

## Future Improvements

- Replace Redis caching of `availableQuantity` with separate caching of static concert info.
- Add a scheduled job to automatically change `PENDING` → `EXPIRED` bookings.
- Implement refresh token rotation / blacklist.
- Introduce a message queue (RabbitMQ, Kafka) for asynchronous payment handling.
- Add rate limiting with fallback (Redis + Caffeine).

See [`docs/Design.md`](./docs/Design.md) for a detailed discussion.

## License & Contact

This project was developed as a technical assessment for a Product Backend Engineer role.  
For any questions, please refer to the `docs/` folder or the provided source code.

**Author:** VŨ HOÀNG CHUNG  
