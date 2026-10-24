import http from 'k6/http';
import { check } from 'k6';

// ==============================================================================
// Scenario: Cold-Cache / Broad Distribution (200 RPS across 10,000 Keys)
// Tests cache miss fallback behavior and PostgreSQL connection pool under
// high-entropy access where few requests hit existing Redis cache keys.
// ==============================================================================

export const options = {
  scenarios: {
    cold_cache_misses: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 30,
      maxVUs: 150,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [
      'p(50)<30',  // 50% under 30ms
      'p(95)<100', // 95% under 100ms
      'p(99)<200', // 99% under 200ms
    ],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

export default function () {
  // Broad selection across 10,000 cold links
  const index = Math.floor(Math.random() * 10000) + 1;
  const code = `cold-${String(index).padStart(5, '0')}`;

  const res = http.get(`${BASE_URL}/${code}`, {
    redirects: 0,
  });

  check(res, {
    'status is 302': (r) => r.status === 302,
    'location header present': (r) => r.headers['Location'] !== undefined,
  });
}
