# ADR 005: Transactional Guest Quotas & Multi-Layer Abuse Defenses

- **Status:** Accepted
- **Deciders:** Backend Architecture Team
- **Date:** 2026-10-24
- **Applies to:** Milestone BE1, BE2, BE5, BE7

---

## Context
A key product differentiator of Urlbit is frictionless onboarding: anonymous visitors can shorten links immediately without signing up. However, anonymous write endpoints are primary targets for spam, phishing, and denial-of-wallet attacks. The system requires strict transactional quota enforcement without locking the entire database table.

## Decision
Urlbit implements **multi-layered guest defenses**:
1. **Server-Issued Guest Identifier:** A persistent 30-day HttpOnly cookie (`urlbit_guest_id`) containing a UUID.
2. **Fixed Max-Age (Never Refreshed):** The cookie `Max-Age` (30 days) is **never extended** on subsequent requests, preventing artificial quota extension.
3. **Atomic PostgreSQL Quota Increment:**
   ```sql
   INSERT INTO guest_quotas (guest_id_hash, successful_creations, created_at, updated_at)
   VALUES (:guestIdHash, 1, NOW(), NOW())
   ON CONFLICT (guest_id_hash)
   DO UPDATE SET successful_creations = guest_quotas.successful_creations + 1, updated_at = NOW()
   WHERE guest_quotas.successful_creations < 5
   RETURNING successful_creations;
   ```
   If the counter reaches 5, the `WHERE` clause fails and the statement returns `null`, throwing `GuestQuotaExceededException` (`429 Too Many Requests`).
4. **IP Subnet Limiting as Primary Abuse Guard:** Because malicious actors can delete browser cookies, the guest cookie is treated as a product quota, **not an abuse defense**. A mandatory distributed IP rate limit (30 creations / 60 seconds per IPv4 / IPv6 `/64`) operates independently in Redis.
5. **Safe Data Purging (`GuestQuotaPurgeJob`):** A daily scheduled job deletes `guest_quotas` rows older than 30 days. Because the cookie lifetime is strictly 30 days, purging rows older than 30 days cleans up orphaned records without ever resetting the counter of an active user.

## Rationale & Tradeoffs

### 1. Why Atomic `ON CONFLICT ... WHERE ... RETURNING` over Pre-Check:
- Checking the quota in a `SELECT` query followed by an `INSERT` creates a race condition: an attacker issuing 10 concurrent requests from the same cookie would see `count = 0` across all 10 threads, allowing all 10 links to be created (violating the 5-link limit).
- PostgreSQL row-level locks on `ON CONFLICT DO UPDATE` guarantee strict serializability for that specific `guest_id_hash` without requiring coarse table locks or distributed locks.

### 2. Cookie Clearing Limitation:
- A user can clear their browser cookies or use incognito mode to receive a fresh `urlbit_guest_id`.
- This is an inherent, well-documented limitation of cookie-based state. Attempting to track users via browser fingerprinting introduces privacy and compliance liabilities (GDPR/ePrivacy).
- The distributed IP rate limiter (IPv4 exact, IPv6 `/64`) acts as the defense-in-depth barrier that prevents mass-automation regardless of cookie manipulation.

## Consequences
- **Positive:** Zero friction for legitimate guests; strict transactional concurrency guarantees; database storage remains bounded by automated 30-day cleanup; multi-layered protection against automated spam.
- **Negative:** Users clearing cookies can create up to 5 more links if under the IP rate limit; acceptable product tradeoff to avoid intrusive tracking.
