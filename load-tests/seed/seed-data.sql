-- ============================================================================
-- Urlbit Load Testing & Benchmark Seed Data
-- Pre-populates benchmark test user and 10,100 short links for k6 scenarios.
-- Idempotent: safe to run multiple times (uses ON CONFLICT DO NOTHING).
-- ============================================================================

-- 1. Benchmark Test User
INSERT INTO users (id, email, password_hash, created_at, updated_at, status, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'benchmarks@urlbit.in',
    -- BCrypt hash for 'BenchmarkPassword123!'
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.I9B.P8pL9s4p9z0wB7M9Cg9C0Y2Gia',
    NOW(),
    NOW(),
    'ACTIVE',
    'USER'
) ON CONFLICT (email) DO NOTHING;

-- 2. 100 Hot Links (codes: hot-0001 to hot-0100)
-- Used for hot-cache benchmarks where requests hit Redis repeatedly.
INSERT INTO short_urls (
    owner_id,
    code,
    original_url,
    created_at,
    updated_at,
    expires_at,
    status,
    disabled_reason,
    last_safety_check_at
)
SELECT
    '00000000-0000-0000-0000-000000000001',
    'hot-' || LPAD(i::text, 4, '0'),
    'https://example.com/destination/hot/' || i,
    NOW(),
    NOW(),
    NOW() + INTERVAL '90 days',
    'ACTIVE',
    NULL,
    NOW()
FROM generate_series(1, 100) AS i
ON CONFLICT (code) DO NOTHING;

-- 3. 10,000 Cold Links (codes: cold-00001 to cold-10000)
-- Used for cold-cache benchmarks where random requests force PostgreSQL lookups.
INSERT INTO short_urls (
    owner_id,
    code,
    original_url,
    created_at,
    updated_at,
    expires_at,
    status,
    disabled_reason,
    last_safety_check_at
)
SELECT
    '00000000-0000-0000-0000-000000000001',
    'cold-' || LPAD(i::text, 5, '0'),
    'https://example.com/destination/cold/' || i,
    NOW(),
    NOW(),
    NOW() + INTERVAL '90 days',
    'ACTIVE',
    NULL,
    NOW()
FROM generate_series(1, 10000) AS i
ON CONFLICT (code) DO NOTHING;
