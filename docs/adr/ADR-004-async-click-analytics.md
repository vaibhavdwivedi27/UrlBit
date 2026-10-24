# ADR 004: Asynchronous Click Analytics Buffering vs. Synchronous Row Locking

- **Status:** Accepted
- **Deciders:** Backend Architecture Team
- **Date:** 2026-10-24
- **Applies to:** Milestone BE6, BE7, BE8

---

## Context
Redirect performance is the primary SLA of the system (< 50ms p95). When a short link goes viral on social media, thousands of concurrent requests arrive simultaneously for the exact same short code. The system must record click totals, daily trends, and referring domains without degrading redirect speed.

## Decision
Urlbit implements **asynchronous, non-blocking click analytics**:
1. The redirect hot path emits a lightweight `ClickEvent` into a bounded in-memory buffer (`ArrayBlockingQueue<ClickEvent>`, capacity: 10,000) using non-blocking `.offer()`.
2. The HTTP redirect response returns immediately (`HTTP 302`) without waiting for database writes.
3. If the queue is saturated, the system enforces an explicit **`OVERFLOW_DROP` backpressure policy**: drops the event, increments the Micrometer `analytics.events.dropped` counter, and allows the redirect to succeed.
4. A background worker (`ClickBatchFlusher`) drains batches up to 1,000 events, aggregates counts in-memory per `(short_url_id, day)` and `(short_url_id, day, referrer_host)`, and executes atomic PostgreSQL batch upserts via `JdbcTemplate.batchUpdate`.
5. On shutdown (`@PreDestroy`), remaining buffered events are drained and flushed to disk.

## Rationale & Tradeoffs

### Why Synchronous `UPDATE short_urls SET click_count = click_count + 1` was REJECTED:
1. **Deadly Hot-Row Lock Contention:** PostgreSQL enforces row-level locks on `UPDATE`. When 2,000 requests per second hit a viral link, all 2,000 requests line up waiting for the exclusive lock on that single row. Latency degrades from 5ms to several seconds, and the connection pool exhausts immediately.
2. **Redirect Tied to DB Health:** If the database undergoes a checkpoint or transient lag, every redirect in the world freezes.
3. **Database Write Amplification:** Executing 2,000 disk writes per second for 2,000 clicks generates massive WAL (Write-Ahead Logging) overhead. Aggregating 2,000 clicks in memory produces just **1 single batch upsert**, reducing database write I/O by 99.9%.

### Why Bounded In-Memory Queue with OVERFLOW_DROP:
- **Zero Redirect Blocking:** An in-memory queue `.offer()` takes < 1 microsecond.
- **Fail-Safe Protection:** An unbounded queue (`LinkedBlockingQueue`) introduces an Out-Of-Memory (OOM) vulnerability if database writes stall. The bounded queue caps memory consumption.
- **Durability Contract:** Counts are explicitly labeled **best-effort at-least-once per batch**. In the event of a catastrophic container kill (`kill -9`), un-flushed events in the buffer may be dropped, which is an acceptable industry tradeoff for a free/tier URL shortening service compared to paying the latency penalty on every redirect.

## Consequences
- **Positive:** Redirect latency remains sub-10ms even under viral load; database write I/O reduced by several orders of magnitude; database pool immune to redirect surges.
- **Negative:** Click counts are eventually consistent (typically updated within 1 second of redirect); best-effort durability on sudden process crash.
