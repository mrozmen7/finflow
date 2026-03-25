# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# FinFlow — Project Rules & Architecture

## Project Overview
FinFlow is an event-driven payment processing and fraud detection backend system.
Built with Java 21 + Spring Boot 4.x + PostgreSQL + Redis + Kafka.

## Commands

Use `make` targets as the primary interface (see `Makefile`):

```bash
make run          # ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
make test         # ./mvnw test
make build        # ./mvnw clean package -DskipTests
make clean        # ./mvnw clean
make lint         # ./mvnw checkstyle:check
make coverage     # runs tests + JaCoCo; report at target/site/jacoco/index.html

make docker-up    # start PostgreSQL + Redis (detached)
make docker-down  # stop containers
make docker-logs  # tail container logs
make docker-reset # wipe volumes and restart
```

For granular test runs not covered by the Makefile:

```bash
# Single test class
./mvnw test -Dtest=TransactionServiceTest

# Single test method
./mvnw test -Dtest=TransactionServiceTest#shouldProcessPayment

# Integration tests only
./mvnw test -Dtest="*IT"
```

## Local Infrastructure

`infra/docker/docker-compose.yml` provides:
- **PostgreSQL 16** on `localhost:5432` — db/user/password all `finflow`
- **Redis 7** on `localhost:6379`

Run `make docker-up` before starting the app locally. Both services have healthchecks.

## Architecture
- **Modular Monolith** with Hexagonal Architecture (Ports & Adapters)
- Three modules: shared, transaction, fraud
- Modules communicate via Kafka events, NOT direct method calls
- Package structure: domain / application / infrastructure / api

## Module Boundaries
- `shared/`: ONLY config, security, exception, logging, common utils — must NOT contain any domain/business logic
- `transaction/`: payment processing, accounts, transfers, outbox
- `fraud/`: Kafka consumer, rule engine, flagged transactions
- Modules must NOT import each other's domain classes directly

## File Patterns
- Entity: `src/main/java/com/finflow/{module}/domain/`
- Use case: `src/main/java/com/finflow/{module}/application/`
- DB/Kafka adapter: `src/main/java/com/finflow/{module}/infrastructure/`
- REST controller: `src/main/java/com/finflow/{module}/api/`
- Flyway: `src/main/resources/db/migration/`

## Code Standards

### Java
- Java 21: Use Records for DTOs, sealed classes for domain types
- No Lombok — use Records and explicit constructors
- All public methods must have Javadoc
- Max method length: 20 lines. If longer, extract.
- No magic numbers — use constants with clear names

> **Note:** `pom.xml` currently includes Lombok as a dependency. This contradicts the "No Lombok" rule and should be removed from `pom.xml`.

### Spring Boot
- Spring Boot 4.x (current: 4.0.4) with spring-boot-starter-parent
- Profiles: dev (default), staging, prod
- Use `@ConfigurationProperties` over `@Value`
- Use constructor injection, NEVER field injection

### Database
- PostgreSQL with Flyway migrations
- Migration naming: `V001__description.sql`
- Every query must use parameterized statements (no string concat)
- Use optimistic locking (`@Version`) on all mutable entities

### API Design
- REST endpoints follow: `/api/v1/{resource}`
- Use `ProblemDetail` (RFC 7807) for error responses
- All endpoints documented with OpenAPI annotations
- Pagination: `page`/`size` parameters, default `page=0`, `size=20`

### Testing
- Test pyramid: unit > integration > e2e
- Unit tests: JUnit 5 + Mockito
- Integration tests: Testcontainers (PostgreSQL, Redis, Kafka)
- Minimum coverage target: 80%
- Test class naming: `{ClassName}Test` (unit), `{ClassName}IT` (integration)

### Git
- Conventional Commits: `feat/fix/test/docs/refactor/chore/perf/ci`
- Format: `type(scope): description`
- Example: `feat(transaction): add outbox pattern for event publishing`
- Branch naming: `feature/FIN-xxx-description`, `bugfix/FIN-xxx`, `setup/xxx`
- Squash merge to main

## Security Rules
- YOLO mode: DISABLED
- JWT authentication required for all `/api/**` endpoints
- Every endpoint must have proper authorization
- Public endpoints (no token required): `POST /api/v1/auth/login`, `POST /api/v1/accounts`, Swagger UI
- Login: `POST /api/v1/auth/login` with `{"username":"admin","password":"admin123"}` → returns `{"token":"...", "tokenType":"Bearer", "expiresIn":86400}`
- Token usage: `Authorization: Bearer <token>` header
- `JwtProperties` (`jwt.secret`, `jwt.expiration`) — configured in application-dev.yml and application-test.yml
- `AuthController` is temporary (hardcoded credentials) — replace when User entity is implemented
- Spring Security classes live in `shared/security`; `JwtProperties` and `OpenApiConfig` live in `shared/config`

## Model Policy
- Default model: Sonnet (CRUD, DTO, Service, Test, Controller)
- Opus: ONLY for architecture decisions, security review, deep debugging

## When Discussing or Using
- Spring Boot, Kafka, Redis → use Context7 MCP for latest docs
- Always check library versions before suggesting dependencies
