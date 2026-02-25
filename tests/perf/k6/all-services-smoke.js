import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = String(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080").trim().replace(/\/+$/, "");
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "10s";
const SMOKE_VUS = Number(__ENV.SMOKE_VUS || 10);
const SMOKE_DURATION = __ENV.SMOKE_DURATION || "60s";
const SMOKE_SLEEP_SECONDS = Number(__ENV.SMOKE_SLEEP_SECONDS || 0.2);
const SMOKE_P95_THRESHOLD_MS = Number(__ENV.SMOKE_P95_THRESHOLD_MS || 1200);
const SMOKE_ERROR_RATE_THRESHOLD = Number(__ENV.SMOKE_ERROR_RATE_THRESHOLD || 0.05);

const smokeLatency = new Trend("services_smoke_latency_ms", true);
const smokeErrorRate = new Rate("services_smoke_error_rate");

function parseOrigin(url) {
  const matched = /^([a-zA-Z][a-zA-Z0-9+.-]*):\/\/([^:/?#]+)(?::(\d+))?/.exec(url || "");
  if (!matched) {
    return { scheme: "http", host: "host.docker.internal", port: "18080" };
  }
  return {
    scheme: matched[1] || "http",
    host: matched[2] || "host.docker.internal",
    port: matched[3] || (matched[1] === "https" ? "443" : "80"),
  };
}

const origin = parseOrigin(BASE_URL);
const defaultTargets = [
  `${BASE_URL}/actuator/health`,
  `${origin.scheme}://${origin.host}:8081/actuator/health`,
  `${origin.scheme}://${origin.host}:8082/actuator/health`,
  `${origin.scheme}://${origin.host}:8083/actuator/health`,
  `${origin.scheme}://${origin.host}:8084/actuator/health`,
  `${origin.scheme}://${origin.host}:8085/actuator/health`,
  `${origin.scheme}://${origin.host}:8086/actuator/health`,
  `${origin.scheme}://${origin.host}:8087/actuator/health`,
  `${BASE_URL}/api/query/users?username=admin`,
  `${BASE_URL}/api/product?page=1&size=2`,
  `${BASE_URL}/api/orders?page=1&size=2`,
  `${BASE_URL}/api/payments?page=1&size=2`,
  `${BASE_URL}/api/stocks?page=1&size=2`,
  `${BASE_URL}/api/search/basic?keyword=demo&page=0&size=2`,
];

const targetRaw = String(__ENV.SERVICE_TARGETS || "").trim();
const targets = targetRaw
  ? targetRaw.split(",").map((item) => item.trim()).filter((item) => item.length > 0)
  : defaultTargets;

export const options = {
  scenarios: {
    services_smoke: {
      executor: "constant-vus",
      vus: SMOKE_VUS,
      duration: SMOKE_DURATION,
      gracefulStop: "5s",
    },
  },
  thresholds: {
    services_smoke_latency_ms: [`p(95)<${SMOKE_P95_THRESHOLD_MS}`],
    services_smoke_error_rate: [`rate<=${SMOKE_ERROR_RATE_THRESHOLD}`],
    http_req_failed: ["rate<=0.08"],
  },
};

export default function run() {
  const requests = targets.map((url) => ["GET", url, null, { timeout: REQUEST_TIMEOUT }]);
  const responses = http.batch(requests);

  responses.forEach((response, idx) => {
    const url = targets[idx];
    const ok = response.status > 0 && response.status < 500 && response.status !== 404;
    smokeLatency.add(response.timings.duration, { target: url });
    smokeErrorRate.add(!ok, { target: url, status: String(response.status || 0) });

    check(response, {
      [`target reachable ${url}`]: () => ok,
    });
  });

  if (SMOKE_SLEEP_SECONDS > 0) {
    sleep(SMOKE_SLEEP_SECONDS);
  }
}
