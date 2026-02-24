import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

const BASE_URL = String(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080")
  .trim()
  .replace(/\/+$/, "");
const ROUTE_VUS = Number(__ENV.ROUTE_VUS || 12);
const ROUTE_DURATION = __ENV.ROUTE_DURATION || "30s";
const ROUTE_SLEEP_SECONDS = Number(__ENV.ROUTE_SLEEP_SECONDS || 0);
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "30s";

const REACHABLE_RESPONSE_CALLBACK = http.expectedStatuses({ min: 200, max: 499 });
const REQUEST_PARAMS = Object.freeze({
  timeout: REQUEST_TIMEOUT,
  responseCallback: REACHABLE_RESPONSE_CALLBACK,
});

const ROUTES = [
  "/api/query/users?username=admin",
  "/api/product?page=1&size=1",
  "/api/orders?page=1&size=1",
  "/api/payments?page=1&size=1",
  "/api/stocks?page=1&size=1",
  "/api/search/basic?keyword=demo&page=0&size=1",
];

const route404Count = new Counter("gateway_route_404_count");
const route404Rate = new Rate("gateway_route_404_rate");
const routeUnhealthyCount = new Counter("gateway_route_unhealthy_count");
const routeUnhealthyRate = new Rate("gateway_route_unhealthy_rate");

export const options = {
  scenarios: {
    gateway_route_probe: {
      executor: "constant-vus",
      vus: ROUTE_VUS,
      duration: ROUTE_DURATION,
      gracefulStop: "5s",
    },
  },
  thresholds: {
    gateway_route_404_rate: ["rate==0"],
    gateway_route_unhealthy_rate: ["rate<=0.05"],
    http_req_failed: ["rate<=0.05"],
  },
};

export default function run() {
  const requests = ROUTES.map((path) => ["GET", `${BASE_URL}${path}`, null, REQUEST_PARAMS]);
  const responses = http.batch(requests);

  responses.forEach((response, idx) => {
    const path = ROUTES[idx];
    const status = response.status;
    const notFound = status === 404;
    const healthy = status > 0 && status < 500 && !notFound;
    if (notFound) {
      route404Count.add(1, { path });
      route404Rate.add(true, { path });
      if (__ENV.DEBUG_ROUTE404 === "1") {
        console.error(`[route-only] 404 path=${path}`);
      }
    } else {
      route404Rate.add(false, { path });
    }
    if (!healthy) {
      routeUnhealthyCount.add(1, { path, status: String(status || 0) });
      routeUnhealthyRate.add(true, { path, status: String(status || 0) });
      if (__ENV.DEBUG_ROUTE_ERRORS === "1") {
        console.error(`[route-only] unhealthy path=${path} status=${status || 0}`);
      }
    } else {
      routeUnhealthyRate.add(false, { path, status: String(status || 0) });
    }

    const checkName = `route ${path} reachable without 5xx/404`;
    check(response, {
      [checkName]: () => healthy,
    });
  });

  if (ROUTE_SLEEP_SECONDS > 0) {
    sleep(ROUTE_SLEEP_SECONDS);
  }
}
