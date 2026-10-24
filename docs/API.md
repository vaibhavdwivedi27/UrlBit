# Urlbit — API Reference Guide

Welcome to the **Urlbit REST API Reference**. This document provides an exhaustive, human-readable guide for all endpoints implemented across the system, including request headers, body schemas, response payloads, rate limits, and RFC 9457 error contracts.

---

## 1. Overview & Base URLs

| Environment | Base URL | Description |
|---|---|---|
| **Local Development** | `http://localhost:8080` | Default local Spring Boot port |
| **Interactive Docs** | `http://localhost:8080/swagger-ui.html` | Swagger UI documentation |
| **OpenAPI Spec (JSON)**| `http://localhost:8080/v3/api-docs` | Raw OpenAPI 3.0 specification |

### Common Headers
- `X-Request-Id`: (Optional) Client-supplied correlation UUID. If omitted, the server automatically generates one. Echoed back in every response header for end-to-end tracing.
- `Content-Type`: `application/json` (for POST/PATCH requests with JSON bodies).

---

## 2. API Summary Table

| Category | Method | Path | Auth Required | Description | Success Status |
|---|---|---|---|---|---|
| **Hot Path** | `GET` | `/{code}` | No | Resolves short code and redirects to original URL | `302 Found` |
| **Auth** | `POST` | `/api/v1/auth/signup` | No | Registers a new user account | `201 Created` |
| **Auth** | `POST` | `/api/v1/auth/login` | No | Authenticates user; issues access JWT + refresh cookie | `200 OK` |
| **Auth** | `POST` | `/api/v1/auth/refresh` | Cookie | Rotates refresh token; issues new access JWT | `200 OK` |
| **Auth** | `POST` | `/api/v1/auth/logout` | Cookie | Revokes refresh token in database & clears cookie | `204 No Content` |
| **Auth** | `GET` | `/api/v1/auth/me` | Bearer JWT | Returns current authenticated user profile | `200 OK` |
| **Links** | `POST` | `/api/v1/urls` | Optional | Creates short link (Guest: quota=5; User: custom alias/expiry) | `201 Created` |
| **Links** | `GET` | `/api/v1/urls` | Bearer JWT | Lists links owned by authenticated user (paginated) | `200 OK` |
| **Links** | `GET` | `/api/v1/urls/{id}` | Bearer JWT | Retrieves short URL metadata by ID | `200 OK` |
| **Links** | `PATCH`| `/api/v1/urls/{id}` | Bearer JWT | Updates link status (`ACTIVE`/`DISABLED`) or expiry | `200 OK` |
| **Links** | `DELETE`| `/api/v1/urls/{id}` | Bearer JWT | Soft-deletes link and purges from Redis cache | `204 No Content` |
| **Analytics**| `GET` | `/api/v1/analytics/urls/{id}` | Bearer JWT | Returns daily click aggregates & top referrer hosts | `200 OK` |
| **Ops** | `GET` | `/actuator/health` | No | Overall system health status (UP / DOWN) | `200 OK` |
| **Ops** | `GET` | `/actuator/health/liveness` | No | Container liveness probe | `200 OK` |
| **Ops** | `GET` | `/actuator/health/readiness` | No | Container readiness probe (checks PostgreSQL & Redis) | `200 OK` |
| **Ops** | `GET` | `/actuator/prometheus` | No | Prometheus text exposition metrics scraper | `200 OK` |

---

## 3. Public Redirect Hot Path

### `GET /{code}`
Resolves a 7-character Base62 code or custom alias and redirects to the original destination URL.
- **Cache behavior:** Checked first in Redis (`link:{code}`). On cache miss, queries PostgreSQL and caches the result with TTL.
- **Click analytics:** Emits a non-blocking in-memory click event asynchronously; never blocks redirect latency.
- **Uniform 404 policy:** Returns `404 Not Found` uniformly for unknown, disabled, deleted, and expired links (prevents link enumeration).

#### Response
```http
HTTP/1.1 302 Found
Location: https://example.com/target-page
X-Request-Id: 7e9f3b2a-14d8-4f21-9952-4a7b98d41e20
```

#### Example `curl`
```bash
curl -i http://localhost:8080/my-custom-alias
```

---

## 4. Authentication Endpoints (`/api/v1/auth`)

### 1. Signup — `POST /api/v1/auth/signup`
Creates a new account. The `role` is unconditionally assigned to `USER` (ignoring any user-supplied role inputs for security).

#### Request Body
```json
{
  "email": "developer@example.com",
  "password": "StrongPassword123!"
}
```

#### Response (`201 Created`)
Sets an HttpOnly, SameSite cookie named `refreshToken`.
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "c4b3a1d2-0000-4000-8000-0123456789ab",
    "email": "developer@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-10-24T12:00:00Z"
  }
}
```

---

### 2. Login — `POST /api/v1/auth/login`
Authenticates with email and password. Protected by a distributed sliding-window rate limit (10 attempts per 5 minutes per IP + email hash).

#### Request Body
```json
{
  "email": "developer@example.com",
  "password": "StrongPassword123!"
}
```

#### Response (`200 OK`)
Sets `refreshToken` HttpOnly cookie and returns new `accessToken`.

---

### 3. Refresh Token — `POST /api/v1/auth/refresh`
Performs **refresh token rotation**: reads the `refreshToken` HttpOnly cookie, validates against PostgreSQL, revokes it, and issues a fresh token pair.
- **Replay Detection:** If a revoked refresh token is presented, the entire token family is immediately invalidated to block token theft.

#### Request Headers
```http
Cookie: refreshToken=<opaque-token-string>
```

#### Response (`200 OK`)
Returns fresh `accessToken` and sets updated `refreshToken` cookie.

---

### 4. Logout — `POST /api/v1/auth/logout`
Revokes the refresh token in PostgreSQL and clears the browser cookie.

#### Response (`204 No Content`)

---

### 5. Profile — `GET /api/v1/auth/me`
Fetches the current authenticated user profile.

#### Request Headers
```http
Authorization: Bearer <accessToken>
```

#### Response (`200 OK`)
```json
{
  "id": "c4b3a1d2-0000-4000-8000-0123456789ab",
  "email": "developer@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-10-24T12:00:00Z"
}
```

---

## 5. Short URL Management (`/api/v1/urls`)

### 1. Create Short URL — `POST /api/v1/urls`
Supports two distinct creation modes:

#### A. Guest Mode (Unauthenticated)
- Does not provide `Authorization` header.
- Automatically receives a fixed 30-day `urlbit_guest_id` cookie.
- Quota: **Maximum 5 lifetime creations** (transactionally enforced in PostgreSQL via `INSERT ... ON CONFLICT DO UPDATE WHERE successful_creations < 5 RETURNING`).
- Custom aliases and custom expiry dates are rejected (`400 Bad Request`).
- Default expiration: 7 days.
- Rate limited: 30 requests / 60 seconds per IP (IPv4 exact, IPv6 `/64` prefix).

#### B. Authenticated Mode
- Provide `Authorization: Bearer <token>`.
- Allows custom alphanumeric aliases (4–32 characters, regex `^[a-zA-Z0-9_-]{4,32}$`).
- Allows custom future expiration timestamp (`expiresAt`).
- Full ownership attached for updates, deletion, and analytics.

#### Request Body
```json
{
  "longUrl": "https://github.com/vaibhavdwivedi27/UrlBit",
  "customAlias": "urlbit-repo",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

#### Response (`201 Created`)
```json
{
  "id": 42,
  "code": "urlbit-repo",
  "shortUrl": "https://urlbit.in/urlbit-repo",
  "longUrl": "https://github.com/vaibhavdwivedi27/UrlBit",
  "status": "ACTIVE",
  "expiresAt": "2026-12-31T23:59:59Z",
  "createdAt": "2026-10-24T12:30:00Z"
}
```

---

### 2. List User Links — `GET /api/v1/urls`
Lists links created by the authenticated user with pagination and sorting.

#### Query Parameters
- `page`: Page index (default: `0`).
- `size`: Page size (default: `20`, max: `100`).

#### Response (`200 OK`)
Standard Spring Data Page JSON payload containing `content: [ShortUrlResponse]`.

---

### 3. Get Link Details — `GET /api/v1/urls/{id}`
Retrieves metadata for a short URL by its numeric ID. Requires user ownership.

---

### 4. Update Link — `PATCH /api/v1/urls/{id}`
Updates link status (`ACTIVE` or `DISABLED`) and/or `expiresAt`.
- **Abuse Takedown Guard:** If `disabled_reason = 'ABUSE_TAKEDOWN'`, the link cannot be reactivated or edited via API. Returns `409 Conflict`.
- **Cache Invalidation:** Automatically evicts the code from Redis so redirect changes take effect immediately.

#### Request Body
```json
{
  "status": "DISABLED",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

---

### 5. Delete Link — `DELETE /api/v1/urls/{id}`
Soft-deletes the link (sets status to `DELETED`) and evicts it from Redis.

#### Response (`204 No Content`)

---

## 6. Click Analytics (`/api/v1/analytics`)

### `GET /api/v1/analytics/urls/{id}`
Retrieves aggregated click counts and top referrers for a link owned by the user.

#### Query Parameters
- `from`: UTC Start Date (`YYYY-MM-DD`, default: 30 days ago).
- `to`: UTC End Date (`YYYY-MM-DD`, default: today).
- `limit`: Number of top referrer domains to return (default: `10`, max: `50`).

#### Response (`200 OK`)
```json
{
  "shortUrlId": 42,
  "code": "urlbit-repo",
  "totalClicks": 1284,
  "dailyClicks": [
    { "day": "2026-10-23", "clicks": 540 },
    { "day": "2026-10-24", "clicks": 744 }
  ],
  "topReferrers": [
    { "referrerHost": "github.com", "clicks": 820 },
    { "referrerHost": "news.ycombinator.com", "clicks": 310 },
    { "referrerHost": "direct", "clicks": 154 }
  ]
}
```

---

## 7. Observability & Health Probes

### 1. Health Overview — `GET /actuator/health`
Returns overall application health and statuses of backing storage.
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL", "validationQuery": "isValid()" } },
    "redis": { "status": "UP", "details": { "version": "7.0.0" } }
  }
}
```

### 2. Liveness Probe — `GET /actuator/health/liveness`
Container liveness check (returns `{"status":"UP"}`).

### 3. Readiness Probe — `GET /actuator/health/readiness`
Verifies PostgreSQL and Redis are reachable before routing production traffic.

### 4. Prometheus Metrics Scrape — `GET /actuator/prometheus`
Standard Prometheus exposition format with histogram buckets and percentile distributions:
- `http_server_requests_seconds`: p50, p95, p99 latencies tagged by route template (`uri="/{code:[a-zA-Z0-9_-]+}"`).
- `hikaricp_connections_active`, `hikaricp_connections_idle`: Database connection pool health.
- `cache_hits_total`, `cache_misses_total`: Redis redirect cache performance.
- `analytics_events_processed_total`, `analytics_events_dropped_total`: Non-blocking click worker throughput.

---

## 8. RFC 9457 Problem Details Error Catalog

All API error responses follow the standard **RFC 9457 Problem Details** format with stable URI identifiers:

```json
{
  "type": "https://urlbit.in/problems/alias-taken",
  "title": "Conflict",
  "status": 409,
  "detail": "Alias 'urlbit-repo' is already in use",
  "instance": "/api/v1/urls"
}
```

### Problem Catalog

| Problem Type URI | HTTP Status | Meaning |
|---|---|---|
| `https://urlbit.in/problems/alias-taken` | `409 Conflict` | Requested custom alias or generated code is already claimed |
| `https://urlbit.in/problems/guest-quota-exceeded` | `429 Too Many Requests` | Guest cookie has reached its lifetime limit of 5 links |
| `https://urlbit.in/problems/rate-limited` | `429 Too Many Requests` | Exceeded creation or login sliding rate limit (includes `Retry-After` header) |
| `https://urlbit.in/problems/unsafe-destination` | `400 Bad Request` | Destination URL failed security checks (self-loop, IP literal, or blocklist) |
| `https://urlbit.in/problems/takedown-active` | `409 Conflict` | Link disabled by administrator for abuse; modifications forbidden |
| `https://urlbit.in/problems/validation-error` | `400 Bad Request` | Request DTO validation failed (includes field-level error list) |
| `https://urlbit.in/problems/not-found` | `404 Not Found` | Requested link ID or route does not exist |
| `https://urlbit.in/problems/unauthorized` | `401 Unauthorized` | Missing, expired, or invalid JWT access token |
| `https://urlbit.in/problems/forbidden` | `403 Forbidden` | Access to resource owned by another user denied |
| `https://urlbit.in/problems/internal-error` | `500 Internal Server Error` | Unexpected server exception |
