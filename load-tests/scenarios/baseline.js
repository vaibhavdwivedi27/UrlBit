import http from 'k6/http';
import { check } from 'k6';

// ==============================================================================
// Scenario: Baseline (100 RPS sustained for 5 minutes)
// Establishes the steady-state baseline performance, memory footprint, and
// p50/p95/p99 latency distribution under normal traffic.
// ==============================================================================

export const options = {
  scenarios: {
    baseline_redirects: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.005'], // < 0.5% failure rate
    http_req_duration: [
      'p(50)<20',  // 50% under 20ms
      'p(95)<50',  // 95% under 50ms
      'p(99)<100', // 99% under 100ms
    ],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

export default function () {
  // Select randomly from the 100 hot links
  const index = Math.floor(Math.random() * 100) + 1;
  const paddedIndex = String(index).padStart(4, '0');
  const code = `hot-${paddedIndex}`;

  const res = http.get(`${BASE_URL}/${code}`, {
    redirects: 0,
  });

  check(res, {
    'redirect is 302': (r) => r.status === 302,
    'has location header': (r) => r.headers['Location'] !== undefined,
  });
}
