# Urlbit Performance Engineering & Benchmark Guide

This directory documents the methodology, tools, seed datasets, and run instructions for executing reproducible load tests against Urlbit.

---

## 1. Quickstart — Running Benchmarks on a Dedicated Machine

### Step 1: Clone the Repository
```bash
git clone https://github.com/vaibhavdwivedi27/UrlBit.git
cd Urlbit
```

### Step 2: Start Infrastructure & Monitoring
Start PostgreSQL, Redis, Prometheus, and Grafana:
```bash
docker compose -f infra/compose.local.yml --profile monitoring up -d
```
Verify all containers are healthy:
```bash
docker ps
```

### Step 3: Start the Backend
From the `backend/` directory:
```bash
./mvnw spring-boot:run
```
*(Or use the turnkey production container from BE9: `docker compose -f infra/compose.prod.yml up -d`)*

### Step 4: Pre-populate Database with Benchmark Data
Seed the database with 100 hot links and 10,000 cold links:
```bash
# On Linux / macOS / Git Bash:
./load-tests/seed/seed.sh

# On Windows:
.\load-tests\seed\seed.bat
```

### Step 5: Execute k6 Scenarios
Run scenarios via the official `grafana/k6` Docker container (no k6 installation required):

```bash
# 1. Quick Smoke Test (5 VUs, 15 seconds)
./load-tests/run-k6.sh smoke.js

# 2. Steady Baseline Test (100 RPS for 5 minutes)
./load-tests/run-k6.sh baseline.js

# 3. Stepped Ramp-up Test (100 -> 500 -> 1,000 -> 2,000 RPS)
./load-tests/run-k6.sh ramp.js

# 4. Hot-Cache Viral Test (1,000 RPS on top 20 links - Redis read speed)
./load-tests/run-k6.sh hot-cache.js

# 5. Cold-Cache Miss Test (200 RPS across 10,000 links - PostgreSQL fallback)
./load-tests/run-k6.sh cold-cache.js

# 6. Create API Write Test (50 link creations / second)
./load-tests/run-k6.sh create-api.js
```

---

## 2. Live Monitoring During Benchmarks

While k6 runs, open Grafana in your browser:
- **URL:** `http://localhost:3000`
- **Credentials:** Username `admin` / Password `admin`
- **Dashboard:** Navigate to **Urlbit System Overview**

The dashboard automatically visualizes:
1. **HTTP Throughput (RPS)**: Real-time requests per second segmented by status code (200, 302, 404, 429).
2. **Latency Percentiles**: Live p50, p95, and p99 response times for redirects and API calls.
3. **Database Connections**: HikariCP active, idle, and pending threads.
4. **Click Analytics Worker**: Events processed per second and drop count.
5. **JVM Memory**: Live heap utilization and garbage collection activity.

---

## 3. SLA Targets & Acceptance Criteria (per Spec §15)

| Metric | Target | Stretch Goal |
|---|---|---|
| **Sustained Redirect RPS** | **1,000 RPS** (over 10 minutes) | **2,000 – 5,000 RPS** |
| **Hot-Cache Latency (p95)** | **< 30 ms** | **< 15 ms** |
| **Hot-Cache Latency (p99)** | **< 70 ms** | **< 35 ms** |
| **Cold-Cache Latency (p95)**| **< 100 ms** | **< 60 ms** |
| **Unexpected Error Rate** | **< 0.5%** | **< 0.1%** |
| **Cache Hit Ratio (Hot)** | **> 98%** | **> 99.5%** |

---

## 4. Recording Benchmark Results
Copy `BENCHMARK_TEMPLATE.md` to a new report file (e.g., `results-2026-10-24.md`) and paste the k6 console summary alongside machine specifications.
