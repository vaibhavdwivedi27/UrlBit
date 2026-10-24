# Urlbit — Performance Engineering & Load Test Plan

**Version:** 1.0  
**Scope:** Milestone BE8 (Performance Experiments & Benchmarking)  
**Target System:** Urlbit Modular Monolith (`GET /{code}`, `POST /api/v1/urls`, Async Analytics)

---

## 1. Objectives & Executive Summary

The primary objective of this load testing plan is to empirically measure, validate, and document the throughput, latency, concurrency, and degradation characteristics of Urlbit under controlled synthetic workloads.

### Primary Goals:
1. **Validate Hot-Path Redirect SLA:** Prove that `GET /{code}` sustains $\ge 1,000\text{ RPS}$ with $p95 < 30\text{ ms}$ and $p99 < 70\text{ ms}$ when Redis cache hit ratio exceeds $98\%$.
2. **Measure Cold-Cache DB Saturation:** Quantify PostgreSQL connection pool (HikariCP) behavior and query latency when cache misses force relational database lookups ($200\text{ RPS}$ across $10,000$ links).
3. **Verify Asynchronous Analytics Non-Blocking Contract:** Confirm that the in-memory bounded queue (`ArrayBlockingQueue`) and scheduled batch flusher decouple redirect latency from database write I/O.
4. **Stress Distributed Abuse Guards:** Verify that atomic Redis sliding-window Lua limiters prevent unauthorized write volume without degrading legit traffic.
5. **Establish Baseline for Portfolio Evidence:** Collect reproducible, un-fabricated benchmark numbers for interview defense.

### Non-Goals:
- Testing distributed multi-region latency (all tests execute in a single deployment network).
- Frontend UI stress testing (Milestone FE1 is isolated from backend SLA validation).
- Fabricating unrealistic claims (e.g. claiming "10,000 RPS" without disclosed hardware logs).

---

## 2. Test Environment & Architecture Under Test

```
+-------------------------------------------------------------+
|                  k6 Load Generator (Docker)                 |
|             (Generates synthetic HTTP/1.1 traffic)          |
+-------------------------------------------------------------+
                               |
                               v HTTP / GET / POST
+-------------------------------------------------------------+
|               Urlbit Spring Boot Backend (JRE 21)           |
|  - Embedded Tomcat (Max Threads: 200)                       |
|  - HikariCP Connection Pool (Max: 20 connections)           |
|  - Async Click Event Buffer (Capacity: 10,000 events)       |
+-------------------------------------------------------------+
               |                               |
        (Read/Write)                    (Batch Upsert)
               v                               v
+----------------------------+   +----------------------------+
|        Redis 7 Alpine      |   |     PostgreSQL 16 Alpine   |
| - Cache (link:{code})      |   | - Authoritative Tables     |
| - Sliding-Window Lua State |   | - url_click_daily          |
+----------------------------+   +----------------------------+
               ^                               ^
               |               Scrape          |
+-------------------------------------------------------------+
|           Prometheus v2.53.0 + Grafana 11.0.0               |
+-------------------------------------------------------------+
```

### Resource Profiles
- **Backend JVM:** Container limit `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`
- **Database:** PostgreSQL 16 (shared buffers 128MB, max connections 100)
- **Cache:** Redis 7 (maxmemory 256MB, volatile-lru eviction)
- **Connection Pool:** HikariCP fixed at 20 connections, connection timeout 3,000ms

---

## 3. Workload Scenarios & Test Suite Matrix

The suite consists of 6 discrete scenarios located in `load-tests/scenarios/`:

| # | Scenario Script | Primary Focus | Traffic Pattern | Target Load | Duration | Acceptance Criteria |
|---|---|---|---|---|---|---|
| **1** | `smoke.js` | Pipeline Sanity | Constant VUs | 5 VUs (~25 RPS) | 15s | 100% 302/200, 0% errors, $p99 < 50\text{ ms}$ |
| **2** | `baseline.js` | Steady-State Capacity | Arrival-rate | 100 RPS | 5 min | $p95 < 20\text{ ms}$, $p99 < 50\text{ ms}$, errors $< 0.1\%$ |
| **3** | `ramp.js` | Breaking Point Discovery | Stepped Stages | $100 \rightarrow 500 \rightarrow 1,000 \rightarrow 2,000\text{ RPS}$ | 10 min | Graceful degradation, no 500 errors, recover after peak |
| **4** | `hot-cache.js` | Redis Hot-Path Read SLA | Arrival-rate | 1,000 RPS (top 20 links) | 3 min | $p95 < 15\text{ ms}$, $p99 < 35\text{ ms}$, hit ratio $> 99\%$ |
| **5** | `cold-cache.js` | PostgreSQL DB Fallback | Arrival-rate | 200 RPS (10,000 links) | 3 min | $p95 < 100\text{ ms}$, HikariCP pending threads $\approx 0$ |
| **6** | `create-api.js` | Write Path & Guest Limiter | Arrival-rate | 50 creates/sec | 2 min | 201 Created within quota, 429 when quota/rate hit |

---

## 4. Dataset Pre-population & Key Distribution

To avoid benchmarking against an empty database, the dataset follows a **Zipfian / Pareto (80/20)** traffic distribution:

- **Seed Script:** `load-tests/seed/seed-data.sql` (executed via `seed.bat` / `seed.sh`)
- **Benchmark User:** `benchmarks@urlbit.in` (Password: `BenchmarkPassword123!`)
- **Hot Links (100 links):** `hot-0001` through `hot-0100` (represents viral content; targeted by `hot-cache.js` and `baseline.js`).
- **Cold Links (10,000 links):** `cold-00001` through `cold-10000` (represents long-tail historical links; targeted by `cold-cache.js`).

---

## 5. Execution Order & Protocol

To ensure clean, unpolluted metric streams, execute scenarios in this strict order:

```
[Step 1: Fresh Start]
       |
       v
[Step 2: Seed Data (10,100 links)]
       |
       v
[Step 3: Smoke Test (smoke.js)] --------> Verify 0 errors
       |
       v
[Step 4: Hot-Cache Warmup & Benchmark (hot-cache.js)]
       |
       v
[Step 5: Steady Baseline Test (baseline.js)]
       |
       v
[Step 6: Stepped Capacity Ramp (ramp.js)]
       |
       v
[Step 7: Cold-Cache DB Miss Test (cold-cache.js)]
       |
       v
[Step 8: URL Creation Write Test (create-api.js)]
       |
       v
[Step 9: Chaos & Resilience Drill (Optional: Redis Pause)]
```

---

## 6. Live Observability & Metrics to Monitor

Open Grafana at `http://localhost:3000` (`admin` / `admin`) $\rightarrow$ **Urlbit System Overview**.

| Panel / Metric | Prometheus Expression | Healthy Range | Red Flag / Alarm Condition |
|---|---|---|---|
| **HTTP Throughput** | `sum(rate(http_server_requests_seconds_count[1m])) by (status)` | Steady matching k6 target | Spike in `500` or unexpected `404` |
| **Redirect Latency p95** | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/{code:[a-zA-Z0-9_-]+}"}[1m])) by (le))` | $< 30\text{ ms}$ | $> 100\text{ ms}$ indicates pool exhaustion |
| **Redirect Latency p99** | `histogram_quantile(0.99, ...)` | $< 70\text{ ms}$ | $> 250\text{ ms}$ |
| **HikariCP Active Connections** | `hikaricp_connections_active` | $2 - 12$ connections | Stays pegged at $20$ (pool starvation) |
| **HikariCP Pending Threads** | `hikaricp_connections_pending` | $0$ | $> 5$ threads waiting for connection |
| **Analytics Buffer Queue** | `analytics_queue_size` | $< 2,000$ items | Approaching $10,000$ (saturation) |
| **Analytics Event Drops** | `rate(analytics_events_dropped_total[1m])` | $0$ | $> 0$ (flusher lagging behind load) |
| **JVM Heap Used** | `jvm_memory_used_bytes{area="heap"}` | Sawtooth pattern | Monotonic rise without drops (memory leak) |

---

## 7. Failure & Chaos Scenarios (Resilience Drills)

During the steady baseline test (`baseline.js`), execute these drills to record resilience evidence:

### Drill A: Redis Outage (Fail-Open Verification)
1. While `baseline.js` is running at 100 RPS, pause Redis:
   ```bash
   docker pause urlbit-prod-redis
   ```
2. **Expected Behavior:**
   - Redirect requests continue to return `302 Found`.
   - Latency increases from $~5\text{ ms}$ to $~25\text{ ms}$ as requests fall back to PostgreSQL.
   - Zero `500 Internal Server Error` responses returned.
3. Unpause Redis:
   ```bash
   docker unpause urlbit-prod-redis
   ```
   - Cache begins repopulating; latency drops back to $< 5\text{ ms}$.

### Drill B: Analytics Queue Saturation
- Run `ramp.js` at peak 2,000 RPS.
- Verify that when click throughput exceeds the batch flusher rate, events are dropped with `analytics.events.dropped` incrementing, but **zero redirects fail or slow down**.

---

## 8. Post-Test Reporting & Archiving

After completing the benchmark runs:

1. Copy the report template:
   ```bash
   cp docs/benchmarks/BENCHMARK_TEMPLATE.md docs/benchmarks/results-$(date +%Y-%m-%d).md
   ```
2. Fill in:
   - Hardware specifications (CPU cores, RAM, OS, Disk type).
   - Observed k6 console outputs (`http_req_duration`, `http_reqs`, `http_req_failed`).
   - Grafana screenshot or peak percentile metrics.
   - Identified bottlenecks and architectural observations.
3. If clean reset of test data is needed, follow the cleanup commands in [`docs/benchmarks/README.md`](../docs/benchmarks/README.md#cleanup).
