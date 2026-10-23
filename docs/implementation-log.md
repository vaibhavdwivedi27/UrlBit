# Implementation Log

## Milestone BE1 — Backend bootstrap and persistence
- **Date:** 2026-10-23
- **Status:** Completed
- **Features completed:**
  - Project scaffolded with Java 21 and Spring Boot 3.3.5.
  - Committed Maven Wrapper (`./mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) with pinned Maven 3.9.9.
  - Spotless code formatting plugin configured with Google Java Format (AOSP style).
  - PostgreSQL 16 Compose service and sample environment (`infra/compose.yml`, `infra/.env.example`).
  - Flyway migration `V1__init_schema.sql` implementing `users`, `short_urls`, `guest_quotas`, `refresh_tokens`, and `url_click_daily` with UTC timestamps (`TIMESTAMPTZ`) and explicit table constraints.
  - `RequestIdFilter` for MDC correlation ID (`requestId`) and `X-Request-Id` response header.
  - RFC 9457 `ProblemDetail` central handling with `ProblemType` URI catalog.
  - Actuator health check and Springdoc OpenAPI (`/swagger-ui.html`, `/v3/api-docs`).
  - Testcontainers PostgreSQL singleton harness (`AbstractIntegrationTest`) with clean integration test execution (`FlywayMigrationIT`, `UrlbitApplicationIT`).
- **Commands & tests executed:**
  - `.\mvnw.cmd spotless:check`: PASS
  - `.\mvnw.cmd compile`: PASS
  - `.\mvnw.cmd verify`: PASS (10/10 tests passed against PostgreSQL Testcontainer)
- **Known limitations:**
  - Core URL APIs, redirect logic, and guest quota management not yet implemented (scheduled for BE2).
  - Auth, JWT, and rate limiting deferred to BE3–BE5 per milestone plan.
- **Next milestone:**
  - **BE2 — Core URL APIs, data integrity and concurrency**
