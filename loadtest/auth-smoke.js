import http from 'k6/http';
import { check, sleep } from 'k6';

const base = __ENV.BASE_URL || 'http://127.0.0.1:8080';
export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
  },
};

export default function () {
  const response = http.get(`${base}/api/health');
  check(response, {
    'health endpoint responds 2xx': (r) => r.status >= 200 && r.status < 300,
  });
  sleep(1);
}
