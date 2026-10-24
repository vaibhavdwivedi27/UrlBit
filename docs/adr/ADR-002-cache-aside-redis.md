# ADR 002: Cache-Aside Consistency & Redis Invalidation Strategy

- **Status:** Accepted
- **Deciders:** Backend Architecture Team
- **Date:** 2026-10-24
- **Applies to:** Milestone BE4, BE7, BE8

---

## Context
The public redirect endpoint (`GET /{code}`) is on the critical hot path, targeted to serve thousands of requests per second with sub-50ms latency. Reading directly from PostgreSQL for every redirect creates unmanageable disk I/O and connection pool contention. However, links are editable (status, expiration, abuse takedowns), requiring strong consistency guarantees between Redis and PostgreSQL.

## Decision
Urlbit implements the **Cache-Aside pattern** with Redis as a read optimization:
1. `GET /{code}` reads from Redis key `link:{code}`.
2. On cache miss, reads from PostgreSQL and caches the result with a 1-hour TTL.
3. Negative caching: Unknown or inactive links are cached for 30 seconds to prevent cache-penetration attacks against the database.
4. On link update or deletion (`PATCH` / `DELETE` / takedown), the Redis key is evicted **post-commit**.
5. On Redis outage, the system degrades gracefully by querying PostgreSQL directly (fail-open).

## Rationale & Tradeoffs

### 1. Why Cache-Aside over Write-Through:
- Write-through forces every URL creation to wait for a Redis write before completing, introducing distributed transaction failure modes.
- Many links created by users may never receive viral traffic; caching on read ensures only active, queried links consume Redis RAM.

### 2. Cache Invalidation Post-Commit:
- Evicting the cache key *before* transaction commit creates a race condition: a concurrent read can fetch the stale row from the database (still uncommitted) and re-populate Redis with stale data.
- By invalidating the cache key strictly after PostgreSQL has committed the transaction, we eliminate stale cache resurrection.

### 3. Graceful Outage Fallback (Fail-Open):
- Redis is an in-memory optimization, not the source of truth.
- All Redis read and write operations are wrapped with try-catch blocks in `LinkCacheService`. If Redis experiences a network partition, cluster restart, or command timeout, the application logs a warning and falls back to PostgreSQL, returning HTTP 302 without throwing 500 errors to users (verified in `ResilienceFaultInjectionIT`).

## Consequences
- **Positive:** Sub-10ms redirect latency on cache hits; zero stale reads after takedowns or edits; system remains functional even during Redis failure.
- **Negative:** Cold cache misses incur a PostgreSQL query; cache stampedes on newly viral links are mitigated by short DB query latency and fast cache warming.
