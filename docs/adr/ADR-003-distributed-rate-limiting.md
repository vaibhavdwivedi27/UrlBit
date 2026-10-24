# ADR 003: Distributed Rate Limiting via Atomic Redis Lua Scripts

- **Status:** Accepted
- **Deciders:** Backend Architecture Team
- **Date:** 2026-10-24
- **Applies to:** Milestone BE5, BE7, BE8

---

## Context
Abuse prevention requires enforcing request limits across endpoints (anonymous link creation, login brute-forcing). In a multi-instance deployment, in-memory rate limiters (e.g. Guava or Resilience4j on individual JVMs) fail because an attacker's requests are load-balanced across multiple instances, multiplying the effective limit by the instance count.

## Decision
Urlbit implements **distributed sliding-window rate limiting** using an atomic **Redis Lua script** executed via `EVALSHA`:
- State is stored in a Redis Sorted Set (`ZSET`) per rate limit key.
- The Lua script atomically purges timestamps older than the sliding window (`ZREMRANGEBYSCORE`), checks current count (`ZCARD`), conditionally adds the current timestamp (`ZADD`), and resets TTL (`EXPIRE`).
- Keys:
  - Anonymous URL creation: `rl:create:ip:{resolvedIp}` (30 requests / 60 seconds).
  - Login attempts: `rl:login:{clientIp}:{emailHash}` (10 attempts / 300 seconds).
- IPv6 prefix grouping: IPv6 addresses are masked to `/64` subnets (`Inet6Address` byte masking) to defeat pool rotation attacks.

## Rationale & Tradeoffs

### 1. Why Atomic Lua over Naive Redis `GET` / `INCR`:
- A sequential sequence of `GET`, check value, and `INCR` suffers from classic Time-Of-Check to Time-Of-Use (TOCTOU) race conditions. Concurrent requests between two threads or instances can simultaneously read `count = 29`, conclude the threshold (30) is not reached, and both increment to 31, exceeding the limit.
- Redis executes Lua scripts as a single atomic transaction on the single-threaded Redis engine. No other command can interleave during evaluation.

### 2. Sliding Window vs Fixed Window:
- Fixed-window rate limiting suffers from the "boundary burst" exploit: an attacker can send 30 requests at `XX:00:59` and another 30 requests at `XX:01:00`, sending 60 requests in 2 seconds without triggering the rate limit.
- The sliding-window algorithm evaluates exact timestamps within the last 60 seconds, guaranteeing the rate limit is never exceeded over any arbitrary 60-second slice.

### 3. IPv6 `/64` Masking:
- Standard consumer and cloud IPv6 allocations assign a `/64` subnet (18.4 quintillion addresses) to a single subscriber. Attackers can trivially generate a new IPv6 address for every HTTP request.
- Grouping all addresses sharing the top 64 bits under a single rate limit bucket prevents rotation bypasses while avoiding false positives for other subscribers.

## Consequences
- **Positive:** Strictly atomic across multiple application instances; immune to boundary burst attacks; blocks IPv6 rotation abuse.
- **Negative:** Slightly higher Redis memory footprint than fixed-window counters (storing member timestamps in sorted sets). Mitigated by automatic TTL expiration on the sorted set.
