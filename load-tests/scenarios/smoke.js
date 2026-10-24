import http from 'k6/http';
import { check, sleep } from 'k6';

// ==============================================================================
// Scenario: Smoke Test
// Fast 15-second verification to validate endpoints, redirects, and status
// codes before executing heavy load tests.
// ==============================================================================

export const options = {
  vus: 5,
  duration: '15s',
  thresholds: {
    // Over 99% of requests must succeed without unexpected errors
    http_req_failed: ['rate<0.01'],
    // 95% of requests must respond within 100ms
    http_req_duration: ['p(95)<100'],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

export default function () {
  // 1. Health check
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'health status is 200': (r) => r.status === 200,
    'health is UP': (r) => r.body && r.body.includes('"status":"UP"'),
  });

  // 2. Short redirect check (disable auto-redirect to inspect 302 and Location)
  const redirectRes = http.get(`${BASE_URL}/hot-0001`, {
    redirects: 0,
  });
  check(redirectRes, {
    'redirect status is 302': (r) => r.status === 302,
    'location header present': (r) => r.headers['Location'] !== undefined,
  });

  sleep(0.5);
}
