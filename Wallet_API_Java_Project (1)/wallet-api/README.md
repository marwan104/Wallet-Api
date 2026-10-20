# Wallet API

A RESTful digital wallet API built with **Java 21** and **Spring Boot 3**. Users register, receive an empty wallet, and can deposit, withdraw, and transfer money to other wallets — with a complete transaction audit trail and JWT-secured endpoints.

> Personal project by Marwan Ahmed — built to demonstrate a production-style Spring Boot backend: layered architecture, Spring Security with JWT, Spring Data JPA, bean validation, global exception handling, and automated tests.

## Features

- **JWT authentication** — stateless security with BCrypt password hashing and role-based access (`USER` / `ADMIN`)
- **Wallet operations** — deposit, withdraw, and atomically transfer money between wallets
- **Audit trail** — every operation writes a transaction record with the balance *after* the operation and the counterparty email for transfers
- **Atomic transfers** — debit and credit happen inside one database transaction (`@Transactional`), so money can never disappear mid-transfer
- **Business rules** — insufficient balance (409), self-transfer (409), unknown recipient (404), duplicate email (409)
- **Paginated history** — transaction history is sorted newest-first and paginated
- **API documentation** — interactive Swagger UI with bearer-auth support
- **Global exception handling** — consistent JSON error responses (400/401/403/404/409/500)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation) |
| Security | Spring Security + JJWT (JWT), BCrypt |
| Persistence | Spring Data JPA (Hibernate) |
| Database | PostgreSQL (Docker Compose) — H2 in-memory for instant local runs |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, MockMvc |
| Packaging | Docker multi-stage build + Docker Compose |

## API Overview

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | - | Register (creates an empty wallet) and return a JWT |
| POST | `/api/v1/auth/login` | - | Login and return a JWT |
| GET | `/api/v1/wallets/me` | JWT | Current user's wallet |
| POST | `/api/v1/wallets/deposit` | JWT | Deposit money |
| POST | `/api/v1/wallets/withdraw` | JWT | Withdraw money |
| POST | `/api/v1/wallets/transfer` | JWT | Atomically transfer to another wallet by email |
| GET | `/api/v1/wallets/transactions` | JWT | Paginated transaction history (`?page=&size=`) |
| GET | `/api/v1/wallets/admin/all` | JWT (ADMIN) | List all wallets |

Interactive docs: **`/swagger-ui.html`** — click *Authorize*, paste your JWT, and call endpoints directly from the browser.

## Getting Started

### Prerequisites
- Java 21+ and Maven 3.9+ (or just Docker)

### Run instantly (H2 in-memory database)
```bash
mvn spring-boot:run
```
The API starts on http://localhost:8080 with a fresh in-memory database. Data resets on restart — perfect for a quick demo.

### Run with PostgreSQL (Docker Compose)
```bash
docker compose up --build
```
This starts PostgreSQL 16 and the API container, wired through environment variables. Data persists in a Docker volume.

### Seeded demo accounts (first startup only)

| Role | Email | Password |
|---|---|---|
| ADMIN | `admin@walletapi.io` | `Admin@1234` |
| USER | `sara@walletapi.io` | `User@1234` (wallet pre-loaded with 500.00) |

## Quick Tour (curl)

```bash
# 1. Register (returns a JWT)
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Omar Test","email":"omar@test.io","password":"Password@1"}'

# 2. Deposit 100
curl -s -X POST http://localhost:8080/api/v1/wallets/deposit \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"amount":100.00}'

# 3. Withdraw 30
curl -s -X POST http://localhost:8080/api/v1/wallets/withdraw \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"amount":30.00}'

# 4. Transfer 20 to another wallet
curl -s -X POST http://localhost:8080/api/v1/wallets/transfer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toEmail":"sara@walletapi.io","amount":20.00}'

# 5. Transaction history (audit trail)
curl -s "http://localhost:8080/api/v1/wallets/transactions?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

## Testing

```bash
mvn test
```

- **Unit tests** (Mockito) — deposit/withdraw/transfer logic, insufficient balance, self-transfer, unknown recipient
- **Integration tests** (MockMvc + H2) — the full journey: register, deposit, withdraw, transfer, paginated audit history, plus security rules (401 for anonymous, 403 for non-admin, 409 for duplicates and overdrafts)

## Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:h2:mem:walletdb;DB_CLOSE_DELAY=-1` | JDBC URL (set to PostgreSQL in Docker Compose) |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DB_USER` / `DB_PASSWORD` | `sa` / empty | Database credentials |
| `JWT_SECRET` | demo value | **Change in production** — HMAC key (min 32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24h) |
| `PORT` | `8080` | HTTP port |
| `DDL_AUTO` | `update` | Hibernate schema handling |

## Deployment Notes

The included `Dockerfile` (multi-stage Maven build) deploys as-is to any container platform (Render, Koyeb, Railway, Fly.io, a VPS, ...). For a free-tier deployment: create the service from the Git repository, let the platform detect the Dockerfile, and set `JWT_SECRET` plus the `DB_*` variables (a hosted PostgreSQL such as Neon works well).

## Project Structure

```
src/main/java/com/marwan/walletapi/
├── WalletApiApplication.java
├── common/          # API error model, global exception handler, exceptions
├── config/          # OpenAPI (Swagger) config, demo data seeder
├── security/        # JWT service, auth filter, security config, user details
├── user/            # User entity, auth service/controller, DTOs
├── wallet/          # Wallet entity, service, controller, DTOs
└── transaction/     # Transaction entity, repository, DTOs
```

## License

MIT — see [LICENSE](LICENSE).
