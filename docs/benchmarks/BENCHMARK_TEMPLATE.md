# Urlbit Benchmark Execution Report

- **Date:** YYYY-MM-DD
- **Engineer:** 
- **Milestone:** BE8 — Performance experiments and optimization
- **Git Commit:** 

---

## 1. Hardware & Environment Specifications

| Component | Specification |
|---|---|
| **Machine Model** | (e.g. Dedicated Server, AWS c6i.2xlarge, Home Lab) |
| **CPU** | (e.g. AMD Ryzen 9 5900X / Intel Core i7 / 8 vCPUs) |
| **RAM** | (e.g. 32 GB DDR4) |
| **Operating System** | (e.g. Ubuntu 22.04 LTS / Windows 11 / macOS) |
| **Docker Engine Version** | (e.g. 27.0.3) |
| **JVM Version** | (e.g. Eclipse Temurin 21.0.4) |
| **Network Setup** | (e.g. Localhost / 1 Gbps LAN / AWS VPC) |

---

## 2. Test Execution Summary

### Scenario 1: Baseline (100 RPS Sustained)
- **Duration:** 5 minutes (30,000 requests)
- **Target RPS:** 100
- **Achieved RPS:** 
- **p50 Latency:** 
- **p95 Latency:** 
- **p99 Latency:** 
- **Max Latency:** 
- **Error Rate (%):** 
- **Observed Bottlenecks:** None

### Scenario 2: Stepped Ramp-Up (100 -> 2,000 RPS)
- **Duration:** 13 minutes
- **Max Achieved RPS:** 
- **Knee Point (Latency starts climbing):** 
- **p95 at 1,000 RPS:** 
- **p99 at 1,000 RPS:** 
- **p95 at 2,000 RPS:** 
- **p99 at 2,000 RPS:** 
- **HikariCP Max Connections Used:** 
- **Redis Memory Usage:** 

### Scenario 3: Hot-Cache / Viral Link (1,000 RPS)
- **Duration:** 3 minutes
- **Achieved RPS:** 
- **Cache Hit Ratio (%):** 
- **p50 Latency:** 
- **p95 Latency:** 
- **p99 Latency:** 
- **Error Rate (%):** 

### Scenario 4: Cold-Cache / Broad Distribution (200 RPS across 10,000 Links)
- **Duration:** 3 minutes
- **Cache Miss Rate (%):** 
- **p95 Latency:** 
- **p99 Latency:** 
- **HikariCP Active Connections:** 
- **PostgreSQL CPU Utilization:** 

### Scenario 5: URL Creation Write Benchmark (50 creates/sec)
- **Duration:** 2 minutes (6,000 creations)
- **Achieved RPS:** 
- **p95 Latency:** 
- **p99 Latency:** 
- **Base62 Collisions Observed:** 
- **Error Rate (%):** 

---

## 3. Raw k6 Output Logs

```text
// Paste k6 terminal summary output here
```

---

## 4. Key Takeaways & Observations

1. **Redis Hot-Path Efficiency:** (Record observations)
2. **PostgreSQL Cache-Miss Resilience:** (Record observations)
3. **Connection Pool Saturation:** (Record observations)
4. **Click Analytics Non-Blocking Buffer:** (Record whether any drop events occurred)
