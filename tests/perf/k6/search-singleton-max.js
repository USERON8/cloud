import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = String(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080").replace(/\/+$/, "");
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "5s";

const TARGET_P95_MS = Number(__ENV.SEARCH_MAX_P95_THRESHOLD_MS || 750);
const TARGET_ERROR_RATE = Number(__ENV.SEARCH_MAX_ERROR_RATE_THRESHOLD || 0.03);
const TARGET_TIMEOUT_RATE = Number(__ENV.SEARCH_MAX_TIMEOUT_RATE_THRESHOLD || 0.08);
const TIMEOUT_MS = Number(__ENV.SEARCH_MAX_TIMEOUT_MS || 900);

const searchLatency = new Trend("search_singleton_latency_ms", true);
const searchErrorRate = new Rate("search_singleton_error_rate");
const searchTimeoutRate = new Rate("search_singleton_timeout_rate");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    singleton_max: {
      executor: "ramping-arrival-rate",
      timeUnit: "1s",
      preAllocatedVUs: Number(__ENV.SEARCH_MAX_PRE_ALLOCATED_VUS || 40),
      maxVUs: Number(__ENV.SEARCH_MAX_MAX_VUS || 180),
      stages: [
        { target: Number(__ENV.SEARCH_MAX_STAGE1_RATE || 25), duration: __ENV.SEARCH_MAX_STAGE1_DURATION || "40s" },
        { target: Number(__ENV.SEARCH_MAX_STAGE2_RATE || 45), duration: __ENV.SEARCH_MAX_STAGE2_DURATION || "40s" },
        { target: Number(__ENV.SEARCH_MAX_STAGE3_RATE || 70), duration: __ENV.SEARCH_MAX_STAGE3_DURATION || "50s" },
        { target: Number(__ENV.SEARCH_MAX_STAGE4_RATE || 90), duration: __ENV.SEARCH_MAX_STAGE4_DURATION || "50s" },
        { target: 0, duration: __ENV.SEARCH_MAX_COOLDOWN_DURATION || "15s" },
      ],
      exec: "runSearch",
    },
  },
  thresholds: {
    search_singleton_latency_ms: [`p(95)<${TARGET_P95_MS}`],
    search_singleton_error_rate: [`rate<=${TARGET_ERROR_RATE}`],
    search_singleton_timeout_rate: [`rate<=${TARGET_TIMEOUT_RATE}`],
    http_req_failed: [`rate<=${TARGET_ERROR_RATE}`],
  },
};

const keywords = ["phone", "iphone", "book", "watch", "shoe", "laptop", "camera", "dress"];

function pickKeyword() {
  return keywords[__ITER % keywords.length];
}

function call(url) {
  const response = http.get(url, { timeout: REQUEST_TIMEOUT });
  const duration = response?.timings?.duration || 0;
  const success = response.status === 200;
  searchLatency.add(duration);
  searchTimeoutRate.add(duration > TIMEOUT_MS);
  searchErrorRate.add(!success);
  return response;
}

export function runSearch() {
  const keyword = encodeURIComponent(pickKeyword());
  const page = __ITER % 5;

  const response1 = call(`${BASE_URL}/api/search/smart-search?keyword=${keyword}&page=${page + 1}&size=16`);
  const response2 = call(`${BASE_URL}/api/search/suggestions?keyword=${keyword}&size=10`);
  const response3 = call(`${BASE_URL}/api/search/search?keyword=${keyword}&page=${page}&size=16`);

  check([response1, response2, response3], {
    "search singleton max all endpoints status=200": (arr) => arr.every((r) => r.status === 200),
  });

  sleep(Number(__ENV.SEARCH_MAX_SLEEP_SECONDS || 0.05));
}
