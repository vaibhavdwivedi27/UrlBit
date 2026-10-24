import http from 'k6/http';
import { check } from 'k6';

// ==============================================================================
// Scenario: Hot-Cache / Viral Link (1,000 RPS on Top 20 Keys)
// Simulates viral social media traffic with high cache repeat rate.
// Measures Redis cache read efficiency and maximum hot-path redirect throughput.
// ==============================================================================

export const options = {
  scenarios: {
    viral_hot_cache: {
      executor: 'constant-arrival-rate',
      rate: 1000,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'], // 99.9% success
    http_req_duration: [
      'p(50)<10', // 50% under 10ms (in-memory Redis speed)
      'p(95)<30', // 95% under 30ms
      'p(99)<70', // 99% under 70ms
    ],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

export default function () {
  // Select strictly from the top 20 viral links
  const index = Math.floor(Math.random() * 20) + 1;
  const code = `hot-${String(index).padStart(4, '0')}`;

  const res = http.get(`${BASE_URL}/${code}`, {
    redirects: 0,
  });

  check(res, {
    'status is 302': (r) => r.status === 302,
    'location header present': (r) => r.headers['Location'] !== undefined,
  });
}
