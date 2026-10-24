import http from 'k6/http';
import { check } from 'k6';

// ==============================================================================
// Scenario: Stepped Ramp-Up (100 -> 500 -> 1,000 -> 2,000 RPS)
// Ramps traffic progressively to discover system throughput limits, latency
// knee points, and connection pool behavior under load.
// Spec target: 1,000 sustained redirect RPS. Stretch: 2,000+ RPS.
// ==============================================================================

export const options = {
  scenarios: {
    ramping_redirects: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 300,
      stages: [
        { target: 100, duration: '1m' },   // Warmup at 100 RPS
        { target: 500, duration: '2m' },   // Step to 500 RPS
        { target: 500, duration: '2m' },   // Sustain 500 RPS
        { target: 1000, duration: '2m' },  // Step to 1,000 RPS (Target)
        { target: 1000, duration: '3m' },  // Sustain 1,000 RPS
        { target: 2000, duration: '1m' },  // Stretch spike to 2,000 RPS
        { target: 2000, duration: '2m' },  // Sustain 2,000 RPS
        { target: 0, duration: '1m' },     // Cooldown
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // Errors under 1%
    http_req_duration: [
      'p(95)<100', // 95% under 100ms
      'p(99)<250', // 99% under 250ms
    ],
  },
};

const BASE_URL = __ENV.TARGET_HOST || 'http://localhost:8080';

export default function () {
  // 90% requests hit top 100 hot links (Redis cache hits)
  // 10% requests hit cold links (database fallback)
  const isHot = Math.random() < 0.9;
  let code;

  if (isHot) {
    const hotIdx = Math.floor(Math.random() * 100) + 1;
    code = `hot-${String(hotIdx).padStart(4, '0')}`;
  } else {
    const coldIdx = Math.floor(Math.random() * 10000) + 1;
    code = `cold-${String(coldIdx).padStart(5, '0')}`;
  }

  const res = http.get(`${BASE_URL}/${code}`, {
    redirects: 0,
  });

  check(res, {
    'status is 302': (r) => r.status === 302,
    'location header present': (r) => r.headers['Location'] !== undefined,
  });
}
