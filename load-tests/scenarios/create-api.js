import http from 'k6/http';
import { check } from 'k6';

// ==============================================================================
// Scenario: URL Creation Write Benchmark (50 creates / sec)
// Benchmarks the write path: JSON deserialization, destination safety checks,
// Base62 random generation, atomic PostgreSQL ON CONFLICT RETURNING inserts,
// and token authentication.
// ==============================================================================

export const options = {
  scenarios: {
    create_links: {
      executor: 'constant-arrival-rate',
      rate: 50,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 15,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [
      'p(95)<150', // Database writes have higher latency than Redis reads
      'p(99)<300',
    ],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

// Setup function runs once before test starts to acquire JWT Bearer token
export function setup() {
  const loginPayload = JSON.stringify({
    email: 'benchmarks@urlbit.in',
    password: 'BenchmarkPassword123!',
  });

  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status !== 200) {
    throw new Error(`Setup failed: Unable to authenticate test user (status ${loginRes.status})`);
  }

  const token = loginRes.json('accessToken');
  return { token: token };
}

export default function (data) {
  // Generate a unique destination URL per iteration
  const randomSuffix = Math.random().toString(36).substring(2, 10);
  const payload = JSON.stringify({
    longUrl: `https://example.com/generated-test-target/${randomSuffix}`,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/urls`, payload, params);

  check(res, {
    'create status is 201': (r) => r.status === 201,
    'has 7-char code': (r) => r.json('code') && r.json('code').length === 7,
    'has canonical shortUrl': (r) => r.json('shortUrl') !== undefined,
  });
}
