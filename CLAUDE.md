# FinFlow — Project Rules & Architecture

## Project Overview
FinFlow is an event-driven payment processing and fraud detection backend system.
Built with Java 21 + Spring Boot 3.x + PostgreSQL + Redis + Kafka.

## Architecture
- **Modular Monolith** with Hexagonal Architecture (Ports & Adapters)
- Three modules: shared, transaction, fraud
- Modules communicate via Kafka events, NOT direct method calls
- Package structure: domain / application / infrastructure / api

## Code Standards

### Java
- Java 21: Use Records for DTOs, sealed classes for domain types
- No Lombok — use Records and explicit constructors
- All public methods must have Javadoc
- Max method length: 20 lines. If longer, extract.
- No magic numbers — use constants with clear names

### Spring Boot
- Spring Boot 3.x with spring-boot-starter-parent
- Profiles: dev (default), staging, prod
- Use @ConfigurationProperties over @Value
- Use constructor injection, NEVER field injection

### Database
- PostgreSQL with Flyway migrations
- Migration naming: V001__description.sql
- Every query must use parameterized statements (no string concat)
- Use optimistic locking (@Version) on all mutable entities

### API Design
- REST endpoints follow: /api/v1/{resource}
- Use ProblemDetail (RFC 7807) for error responses
- All endpoints documented with OpenAPI annotations
- Pagination: page/size parameters, default page=0, size=20

### Testing
- Test pyramid: unit > integration > e2e
- Unit tests: JUnit 5 + Mockito
- Integration tests: Testcontainers (PostgreSQL, Redis, Kafka)
- Minimum coverage target: 80%
- Test class naming: {ClassName}Test (unit), {ClassName}IT (integration)

### Git
- Conventional Commits: feat/fix/test/docs/refactor/chore/perf/ci
- Format: type(scope): description
- Example: feat(transaction): add outbox pattern for event publishing
- Branch naming: feature/FIN-xxx-description, bugfix/FIN-xxx, setup/xxx
- Squash merge to main

## Security Rules
- YOLO mode: DISABLED
- API keys and secrets: NEVER in code or repo
- Use .env files (gitignored) or environment variables
- JWT authentication required for all /api/** endpoints
- Every endpoint must have proper authorization

## Model Policy
- Default model: Sonnet (CRUD, DTO, Service, Test, Controller)
- Opus: ONLY for architecture decisions, security review, deep debugging
- Always prefer Sonnet unless explicitly switching

## Module Boundaries
- shared/: ONLY config, security, exception, logging, common utils
- shared/ must NOT contain any domain/business logic
- transaction/: payment processing, accounts, transfers, outbox
- fraud/: Kafka consumer, rule engine, flagged transactions
- Modules must NOT import each other's domain classes directly

## File Patterns
- Entity: src/main/java/com/finflow/{module}/domain/
- Use case: src/main/java/com/finflow/{module}/application/
- DB/Kafka adapter: src/main/java/com/finflow/{module}/infrastructure/
- REST controller: src/main/java/com/finflow/{module}/api/
- Flyway: src/main/resources/db/migration/

## When Discussing or Using
- Spring Boot, Kafka, Redis → use Context7 MCP for latest docs
- Always check library versions before suggesting dependencies
