import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = String(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080").replace(/\/+$/, "");
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "10s";
const MAIN_TIMEOUT_MS = Number(__ENV.SEARCH_MAIN_TIMEOUT_MS || 700);
const FALLBACK_TIMEOUT_MS = Number(__ENV.SEARCH_FALLBACK_TIMEOUT_MS || 900);
const MAIN_P95_MS = Number(__ENV.SEARCH_MAIN_P95_THRESHOLD_MS || 650);
const FALLBACK_P95_MS = Number(__ENV.SEARCH_FALLBACK_P95_THRESHOLD_MS || 900);
const MAIN_TIMEOUT_RATE = Number(__ENV.SEARCH_MAIN_TIMEOUT_RATE_THRESHOLD || 0.08);
const FALLBACK_TIMEOUT_RATE = Number(__ENV.SEARCH_FALLBACK_TIMEOUT_RATE_THRESHOLD || 0.12);
const MAIN_ERROR_RATE = Number(__ENV.SEARCH_MAIN_ERROR_RATE_THRESHOLD || 0.03);
const FALLBACK_ERROR_RATE = Number(__ENV.SEARCH_FALLBACK_ERROR_RATE_THRESHOLD || 0.05);

const mainLatency = new Trend("search_main_latency_ms", true);
const fallbackLatency = new Trend("search_fallback_latency_ms", true);
const mainTimeoutRate = new Rate("search_main_timeout_rate");
const fallbackTimeoutRate = new Rate("search_fallback_timeout_rate");
const mainErrorRate = new Rate("search_main_error_rate");
const fallbackErrorRate = new Rate("search_fallback_error_rate");

const keywords = ["phone", "apple", "watch", "book", "shoe"];

export const options = {
  thresholds: {
    search_main_latency_ms: [`p(95)<${MAIN_P95_MS}`],
    search_fallback_latency_ms: [`p(95)<${FALLBACK_P95_MS}`],
    search_main_timeout_rate: [`rate<=${MAIN_TIMEOUT_RATE}`],
    search_fallback_timeout_rate: [`rate<=${FALLBACK_TIMEOUT_RATE}`],
    search_main_error_rate: [`rate<=${MAIN_ERROR_RATE}`],
    search_fallback_error_rate: [`rate<=${FALLBACK_ERROR_RATE}`],
    http_req_failed: ["rate<=0.08"],
  },
  scenarios: {
    main_chain: {
      executor: "constant-vus",
      vus: Number(__ENV.SEARCH_MAIN_VUS || 8),
      duration: __ENV.SEARCH_MAIN_DURATION || "45s",
      exec: "mainChain",
      gracefulStop: "5s",
    },
    fallback_chain: {
      executor: "constant-vus",
      vus: Number(__ENV.SEARCH_FALLBACK_VUS || 5),
      duration: __ENV.SEARCH_FALLBACK_DURATION || "45s",
      exec: "fallbackChain",
      startTime: __ENV.SEARCH_FALLBACK_START_TIME || "50s",
      gracefulStop: "5s",
    },
  },
};

function keywordByIteration() {
  return keywords[__ITER % keywords.length];
}

function isResultSuccess(response) {
  if (!response || response.status !== 200) {
    return false;
  }
  try {
    const parsed = response.json();
    return Number(parsed?.code) === 200;
  } catch (_) {
    return false;
  }
}

function trackMetrics(response, p95Metric, timeoutMetric, errorMetric, timeoutMs) {
  const duration = response?.timings?.duration || 0;
  p95Metric.add(duration);
  timeoutMetric.add(duration > timeoutMs);
  errorMetric.add(!isResultSuccess(response));
}

export function mainChain() {
  const keyword = keywordByIteration();
  const params = { timeout: REQUEST_TIMEOUT };
  const requests = [
    ["GET", `${BASE_URL}/api/search/search?keyword=${encodeURIComponent(keyword)}&page=0&size=10`, null, params],
    ["GET", `${BASE_URL}/api/search/smart-search?keyword=${encodeURIComponent(keyword)}&page=1&size=10`, null, params],
    ["GET", `${BASE_URL}/api/search/suggestions?keyword=${encodeURIComponent(keyword)}&size=10`, null, params],
  ];

  const responses = http.batch(requests);
  responses.forEach((response) => trackMetrics(response, mainLatency, mainTimeoutRate, mainErrorRate, MAIN_TIMEOUT_MS));

  check(responses, {
    "main-chain all requests return Result.success": (arr) => arr.every((res) => isResultSuccess(res)),
  });

  sleep(0.5);
}

export function fallbackChain() {
  const keyword = keywordByIteration();
  const params = { timeout: REQUEST_TIMEOUT };
  const requests = [
    ["GET", `${BASE_URL}/gateway/fallback/search?route=search&keyword=${encodeURIComponent(keyword)}`, null, params],
    ["GET", `${BASE_URL}/gateway/fallback/search?route=smart-search&keyword=${encodeURIComponent(keyword)}`, null, params],
    ["GET", `${BASE_URL}/gateway/fallback/search?route=suggestions&keyword=${encodeURIComponent(keyword)}&size=10`, null, params],
  ];

  const responses = http.batch(requests);
  responses.forEach((response) => trackMetrics(response, fallbackLatency, fallbackTimeoutRate, fallbackErrorRate, FALLBACK_TIMEOUT_MS));

  check(responses, {
    "fallback-chain all requests return 200": (arr) => arr.every((res) => res.status === 200),
  });

  sleep(0.5);
}
