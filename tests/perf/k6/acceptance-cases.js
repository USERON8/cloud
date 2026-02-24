import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

function normalizeBaseUrl(rawUrl) {
  const url = String(rawUrl || "").trim().replace(/\/+$/, "");
  return url || "http://host.docker.internal:18080";
}

function parseOrigin(rawUrl) {
  const matched = /^([a-zA-Z][a-zA-Z0-9+.-]*):\/\/([^:/?#]+)(?::(\d+))?/.exec(rawUrl || "");
  if (!matched) {
    return {
      scheme: "http",
      host: "host.docker.internal",
      port: "18080",
    };
  }

  const [, scheme, host, port] = matched;
  return {
    scheme: scheme || "http",
    host: host || "host.docker.internal",
    port: port || (scheme === "https" ? "443" : "80"),
  };
}

function buildDefaultHealthTargets(baseUrl) {
  const origin = parseOrigin(baseUrl);
  const gatewayHealth = `${origin.scheme}://${origin.host}:${origin.port}/actuator/health`;
  const servicePorts = [8081, 8082, 8083, 8084, 8085, 8086, 8087];
  const serviceHealth = servicePorts.map(
    (port) => `${origin.scheme}://${origin.host}:${port}/actuator/health`
  );

  return [gatewayHealth, ...serviceHealth];
}

const BASE_URL = normalizeBaseUrl(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080");
const DEFAULT_CASE_VUS = Number(__ENV.CASE_VUS || 1);
const DEFAULT_CASE_DURATION = __ENV.CASE_DURATION || "30s";
const CASE_STAGE_SECONDS = Number(__ENV.CASE_STAGE_SECONDS || 35);
const SEARCH_DELAY_SECONDS = Number(__ENV.SEARCH_DELAY_SECONDS || 2);
const CASE_SLEEP_SECONDS = Number(__ENV.CASE_SLEEP_SECONDS || 1);
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "30s";
const AUTH_LOGIN_MAX_RETRIES = Number(__ENV.AUTH_LOGIN_MAX_RETRIES || 3);
const AUTH_LOGIN_RETRY_SLEEP_SECONDS = Number(__ENV.AUTH_LOGIN_RETRY_SLEEP_SECONDS || 1);

const CASE_SUCCESS_RATE_THRESHOLD = Number(__ENV.CASE_SUCCESS_RATE_THRESHOLD || 0.9);
const HTTP_FAILED_RATE_THRESHOLD = Number(__ENV.HTTP_FAILED_RATE_THRESHOLD || 0.05);
const CASE_DURATION_P95_THRESHOLD_MS = Number(__ENV.CASE_DURATION_P95_THRESHOLD_MS || 5000);
const DEBUG_CASE02 = __ENV.DEBUG_CASE02 === "1";
const REACHABLE_RESPONSE_CALLBACK = http.expectedStatuses({ min: 200, max: 499 });
const REACHABLE_GET_PARAMS = Object.freeze({
  timeout: REQUEST_TIMEOUT,
  responseCallback: REACHABLE_RESPONSE_CALLBACK,
});

const DEFAULT_HEALTH_TARGETS = buildDefaultHealthTargets(BASE_URL);

const GATEWAY_BATCH_REQUESTS = [
  "/api/query/users?username=admin",
  "/api/product?page=1&size=1",
  "/api/orders?page=1&size=1",
  "/api/payments?page=1&size=1",
  "/api/stocks?page=1&size=1",
  "/api/search/basic?keyword=demo&page=0&size=1",
].map((path) => ["GET", `${BASE_URL}${path}`]);

function getLongEnv(name, fallback = 0) {
  const raw = __ENV[name];
  if (!raw) {
    return fallback;
  }

  const parsed = Number(raw);
  if (Number.isNaN(parsed) || parsed <= 0) {
    return fallback;
  }

  return parsed;
}

function getIdEnv(name, fallback = "") {
  const raw = String(__ENV[name] || "").trim();
  if (/^\d+$/.test(raw) && raw !== "0") {
    return raw;
  }

  const fallbackRaw = String(fallback || "").trim();
  if (/^\d+$/.test(fallbackRaw) && fallbackRaw !== "0") {
    return fallbackRaw;
  }

  return "";
}

function parseTargetList(raw) {
  return raw
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

const HEALTH_TARGETS = parseTargetList(__ENV.HEALTH_TARGETS || DEFAULT_HEALTH_TARGETS.join(","));
const HEALTH_BATCH_REQUESTS = HEALTH_TARGETS.map((target) => ["GET", target, null, REACHABLE_GET_PARAMS]);

const TEST_DATA = Object.freeze({
  userId: getIdEnv("USER_ID"),
  shopId: getIdEnv("SHOP_ID"),
  addressId: getIdEnv("ADDRESS_ID", "1"),
  productId: getIdEnv("PRODUCT_ID"),
  categoryId: getIdEnv("CATEGORY_ID", "1"),
  orderId: getIdEnv("ORDER_ID"),
  paymentId: getIdEnv("PAYMENT_ID"),
  insufficientQuantity: Number(__ENV.INSUFFICIENT_QUANTITY || 999999),
});
const ORDER_NO = __ENV.ORDER_NO || "";
const CASE04_AUTH_TOKEN = __ENV.CASE04_AUTH_TOKEN || "";
const CASE07_AUTH_TOKEN = __ENV.CASE07_AUTH_TOKEN || "";
const AUTH_USER_ID = getIdEnv("AUTH_USER_ID");
const AUTH_USER_TYPE_ENV = String(__ENV.AUTH_USER_TYPE || "USER").toUpperCase();

function buildOrderCreateBody(userId) {
  const payload = {
    shopId: TEST_DATA.shopId,
    addressId: TEST_DATA.addressId,
    receiverName: __ENV.RECEIVER_NAME || "k6 user",
    receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
    receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
    payType: Number(__ENV.PAY_TYPE || 1),
    totalAmount: __ENV.ORDER_TOTAL_AMOUNT || "99.99",
    payAmount: __ENV.ORDER_PAY_AMOUNT || "99.99",
    remark: "k6 acceptance case02",
    orderItems: [
      {
        productId: TEST_DATA.productId,
        productName: __ENV.PRODUCT_NAME || "k6-product",
        price: __ENV.ORDER_ITEM_PRICE || "99.99",
        quantity: Number(__ENV.ORDER_ITEM_QUANTITY || 1),
      },
    ],
  };

  if (userId) {
    payload.userId = userId;
  }

  return JSON.stringify(payload);
}

const REFUND_CREATE_BODY = JSON.stringify({
  orderId: TEST_DATA.orderId,
  orderNo: ORDER_NO,
  refundType: Number(__ENV.REFUND_TYPE || 1),
  refundReason: __ENV.REFUND_REASON || "k6 refund",
  refundDescription: __ENV.REFUND_DESCRIPTION || "k6 acceptance case05",
  refundAmount: __ENV.REFUND_AMOUNT || "1.00",
  refundQuantity: Number(__ENV.REFUND_QUANTITY || 1),
});

const acceptanceCase = new Counter("acceptance_case");
const acceptanceCaseFailed = new Counter("acceptance_case_failed");
const acceptanceCaseSkipped = new Counter("acceptance_case_skipped");
const acceptanceCaseSuccessRate = new Rate("acceptance_case_success_rate");
const acceptanceCaseDurationMs = new Trend("acceptance_case_duration_ms", true);

function scenarioConfig(index, execName, caseId, caseName) {
  const perCaseDuration = __ENV[`CASE${caseId}_DURATION`] || DEFAULT_CASE_DURATION;
  const perCaseVus = Number(__ENV[`CASE${caseId}_VUS`] || DEFAULT_CASE_VUS);

  return {
    executor: "constant-vus",
    exec: execName,
    vus: perCaseVus,
    duration: perCaseDuration,
    gracefulStop: "5s",
    startTime: `${index * CASE_STAGE_SECONDS}s`,
    tags: {
      case_id: caseId,
      case_name: caseName,
    },
  };
}

export const options = {
  insecureSkipTLSVerify: true,
  thresholds: {
    acceptance_case_success_rate: [`rate>=${CASE_SUCCESS_RATE_THRESHOLD}`],
    acceptance_case_duration_ms: [`p(95)<${CASE_DURATION_P95_THRESHOLD_MS}`],
    http_req_failed: [`rate<=${HTTP_FAILED_RATE_THRESHOLD}`],
  },
  scenarios: {
    case_01_gateway_route: scenarioConfig(0, "case01GatewayRoute", "01", "gateway-route"),
    case_02_order_created: scenarioConfig(1, "case02OrderCreated", "02", "order-created"),
    case_03_payment_success: scenarioConfig(2, "case03PaymentSuccess", "03", "payment-success"),
    case_04_stock_insufficient: scenarioConfig(3, "case04StockInsufficient", "04", "stock-insufficient"),
    case_05_refund_flow: scenarioConfig(4, "case05RefundFlow", "05", "refund-flow"),
    case_06_event_idempotency: scenarioConfig(5, "case06EventIdempotency", "06", "event-idempotency"),
    case_07_search_sync: scenarioConfig(6, "case07SearchSync", "07", "search-sync"),
    case_08_regression_baseline: scenarioConfig(7, "case08RegressionBaseline", "08", "regression-baseline"),
  },
};

const DEFAULT_JSON_HEADERS = Object.freeze({ "Content-Type": "application/json" });
const NO_AUTH_PARAMS = Object.freeze({
  headers: DEFAULT_JSON_HEADERS,
  timeout: REQUEST_TIMEOUT,
});
const NO_AUTH_REACHABLE_PARAMS = Object.freeze({
  headers: DEFAULT_JSON_HEADERS,
  timeout: REQUEST_TIMEOUT,
  responseCallback: REACHABLE_RESPONSE_CALLBACK,
});
const authParamsCache = new Map();

function jsonHeaders(token = "", allow4xx = false) {
  if (!token && !allow4xx) {
    return NO_AUTH_PARAMS;
  }
  if (!token && allow4xx) {
    return NO_AUTH_REACHABLE_PARAMS;
  }

  const cacheKey = `${allow4xx ? "reachable" : "strict"}:${token}`;
  const cached = authParamsCache.get(cacheKey);
  if (cached) {
    return cached;
  }

  const params = {
    headers: {
      ...DEFAULT_JSON_HEADERS,
      Authorization: `Bearer ${token}`,
    },
    timeout: REQUEST_TIMEOUT,
  };

  if (allow4xx) {
    params.responseCallback = REACHABLE_RESPONSE_CALLBACK;
  }

  authParamsCache.set(cacheKey, params);
  return params;
}

function parseJsonResponse(response) {
  if (!response) {
    return null;
  }

  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function isResultSuccess(response) {
  if (!response || response.status !== 200) {
    return false;
  }

  const parsed = parseJsonResponse(response);
  return parsed && Number(parsed.code) === 200;
}

function markResult(tags, result) {
  acceptanceCase.add(1, { ...tags, result });
  if (result !== "skipped") {
    acceptanceCaseSuccessRate.add(result === "success", tags);
  }

  if (result === "failed") {
    acceptanceCaseFailed.add(1, tags);
  }

  if (result === "skipped") {
    acceptanceCaseSkipped.add(1, tags);
  }
}

function runCase(caseId, caseName, runFn) {
  const tags = {
    case_id: caseId,
    case_name: caseName,
  };

  const start = Date.now();
  let result = "failed";

  try {
    result = runFn(tags);
  } catch (error) {
    result = "failed";
    console.error(`[case-${caseId}] execution error: ${String(error)}`);
  }

  acceptanceCaseDurationMs.add(Date.now() - start, { ...tags, result });
  markResult(tags, result);
  return result;
}

function sleepBetweenCases() {
  if (CASE_SLEEP_SECONDS > 0) {
    sleep(CASE_SLEEP_SECONDS);
  }
}

function getAuthTokenFromSetup(data) {
  if (data && data.authToken) {
    return data.authToken;
  }

  return "";
}

function getAuthUserIdFromSetup(data) {
  const setupUserId = String(data?.authUserId || "").trim();
  if (setupUserId) {
    return setupUserId;
  }

  if (AUTH_USER_ID) {
    return AUTH_USER_ID;
  }

  if (TEST_DATA.userId) {
    return TEST_DATA.userId;
  }

  return "";
}

function getAuthUserTypeFromSetup(data) {
  const setupUserType = String(data?.authUserType || "").toUpperCase();
  if (setupUserType) {
    return setupUserType;
  }
  return AUTH_USER_TYPE_ENV;
}

function hasMerchantOrAdminRole(userType) {
  return userType === "MERCHANT" || userType === "ADMIN";
}

function shouldRetryLogin(response) {
  if (!response) {
    return true;
  }

  return response.status === 0 || response.status === 429 || response.status >= 500;
}

function isStockInsufficientPayload(payload) {
  if (payload === false) {
    return true;
  }

  if (!payload || typeof payload !== "object") {
    return false;
  }

  return (
    payload.available === false ||
    payload.sufficient === false ||
    payload.hasStock === false ||
    payload.pass === false ||
    payload.result === false
  );
}

function extractUserIdFromLoginResponse(response) {
  const body = String(response?.body || "");
  const patterns = [
    /"user"\s*:\s*\{[\s\S]*?"id"\s*:\s*(\d+)/,
    /"userId"\s*:\s*(\d+)/,
    /"user_id"\s*:\s*(\d+)/,
  ];

  for (const pattern of patterns) {
    const matched = pattern.exec(body);
    if (matched && matched[1]) {
      return matched[1];
    }
  }

  return "";
}

function extractDataIdFromResponse(response) {
  const body = String(response?.body || "");
  const patterns = [/"data"\s*:\s*\{[\s\S]*?"id"\s*:\s*(\d+)/, /"id"\s*:\s*(\d+)/];

  for (const pattern of patterns) {
    const matched = pattern.exec(body);
    if (matched && matched[1]) {
      return matched[1];
    }
  }

  return "";
}

export function setup() {
  if (__ENV.AUTH_TOKEN) {
    return {
      authToken: __ENV.AUTH_TOKEN,
      authUserId: AUTH_USER_ID || TEST_DATA.userId || "",
      authUserType: AUTH_USER_TYPE_ENV,
    };
  }

  const username = __ENV.AUTH_USERNAME;
  const password = __ENV.AUTH_PASSWORD;
  if (!username || !password) {
    return { authToken: "" };
  }

  const loginPayload = {
    username,
    password,
    userType: __ENV.AUTH_USER_TYPE || "USER",
  };

  let lastResponse = null;
  for (let attempt = 1; attempt <= AUTH_LOGIN_MAX_RETRIES; attempt += 1) {
    const loginResponse = http.post(
      `${BASE_URL}/auth/sessions`,
      JSON.stringify(loginPayload),
      jsonHeaders()
    );
    lastResponse = loginResponse;

    if (isResultSuccess(loginResponse)) {
      const parsed = parseJsonResponse(loginResponse);
      const accessToken = parsed?.data?.access_token || parsed?.data?.accessToken || parsed?.data?.token;
      if (accessToken) {
        const responseUserId = extractUserIdFromLoginResponse(loginResponse);
        const responseUserType = String(parsed?.data?.userType || parsed?.data?.user?.userType || "").toUpperCase();
        return {
          authToken: accessToken,
          authUserId: responseUserId,
          authUserType: responseUserType || AUTH_USER_TYPE_ENV,
        };
      }

      console.error("[setup] access token missing in login response");
      return { authToken: "" };
    }

    if (attempt < AUTH_LOGIN_MAX_RETRIES && shouldRetryLogin(loginResponse)) {
      console.warn(
        `[setup] auth login retrying (${attempt}/${AUTH_LOGIN_MAX_RETRIES}), status=${loginResponse.status}`
      );
      if (AUTH_LOGIN_RETRY_SLEEP_SECONDS > 0) {
        sleep(AUTH_LOGIN_RETRY_SLEEP_SECONDS);
      }
      continue;
    }

    break;
  }

  const lastStatus = lastResponse ? lastResponse.status : 0;
  console.error(
    `[setup] auth login failed after ${AUTH_LOGIN_MAX_RETRIES} attempts, last_status=${lastStatus}`
  );
  return { authToken: "" };
}

export function case01GatewayRoute() {
  runCase("01", "gateway-route", () => {
    const gatewayRequests = GATEWAY_BATCH_REQUESTS.map(([method, url]) => [
      method,
      url,
      null,
      REACHABLE_GET_PARAMS,
    ]);
    const responses = http.batch(gatewayRequests);
    const allReachable = responses.every(
      (response) => response.status > 0 && response.status < 500 && response.status !== 404
    );

    check(responses, {
      "case01 routes reachable without 5xx/404": () => allReachable,
    });

    return allReachable ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case02OrderCreated(data) {
  runCase("02", "order-created", () => {
    const authToken = getAuthTokenFromSetup(data);
    const authUserId = getAuthUserIdFromSetup(data);
    if (!authToken || !TEST_DATA.shopId || !TEST_DATA.productId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const createOrderBody = buildOrderCreateBody(authUserId);
    const createOrderResponse = http.post(`${BASE_URL}/api/orders`, createOrderBody, authParams);
    const createOk = isResultSuccess(createOrderResponse);
    if (!createOk && DEBUG_CASE02) {
      const truncatedBody = String(createOrderResponse?.body || "").slice(0, 300);
      console.error(
        `[case-02] create order failed, status=${createOrderResponse?.status}, body=${truncatedBody}`
      );
    }
    check(createOrderResponse, {
      "case02 create order success": () => createOk,
    });

    if (!createOk) {
      return "failed";
    }

    const orderId = extractDataIdFromResponse(createOrderResponse);
    if (!orderId) {
      return "failed";
    }

    const authReachableParams = jsonHeaders(authToken, true);
    const [paymentResponse, stockResponse] = http.batch([
      ["GET", `${BASE_URL}/api/payments/order/${orderId}`, null, authReachableParams],
      ["GET", `${BASE_URL}/api/stocks/product/${TEST_DATA.productId}`, null, authReachableParams],
    ]);

    const paymentReachable = paymentResponse.status !== 404;
    const stockReachable = stockResponse.status !== 404;
    const linkageOk = paymentReachable && stockReachable;

    check(paymentResponse, {
      "case02 payment route reachable": () => paymentReachable,
    });
    check(stockResponse, {
      "case02 stock route reachable": () => stockReachable,
    });

    return linkageOk ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case03PaymentSuccess(data) {
  runCase("03", "payment-success", () => {
    const authToken = getAuthTokenFromSetup(data);
    let paymentId = TEST_DATA.paymentId;
    const orderId = TEST_DATA.orderId;

    if (!authToken) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const tolerantAuthParams = jsonHeaders(authToken, true);
    if (!paymentId && orderId) {
      const paymentByOrder = http.get(`${BASE_URL}/api/payments/order/${orderId}`, authParams);
      paymentId = extractDataIdFromResponse(paymentByOrder);
    }

    if (!paymentId) {
      return "skipped";
    }

    const paySuccessResponse = http.post(
      `${BASE_URL}/api/payments/${paymentId}/success`,
      null,
      tolerantAuthParams
    );

    const paySuccessOk = paySuccessResponse.status === 200 || paySuccessResponse.status === 409;
    check(paySuccessResponse, {
      "case03 mark payment success": () => paySuccessOk,
    });

    if (orderId) {
      const orderStatusResponse = http.get(`${BASE_URL}/api/orders/${orderId}/paid-status`, authParams);
      check(orderStatusResponse, {
        "case03 order paid-status route reachable": (response) => response.status !== 404,
      });
    }

    return paySuccessOk ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case04StockInsufficient(data) {
  runCase("04", "stock-insufficient", () => {
    const authToken = CASE04_AUTH_TOKEN || getAuthTokenFromSetup(data);
    if (!CASE04_AUTH_TOKEN && !hasMerchantOrAdminRole(getAuthUserTypeFromSetup(data))) {
      return "skipped";
    }
    if (!authToken || !TEST_DATA.productId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const response = http.get(
      `${BASE_URL}/api/stocks/check/${TEST_DATA.productId}/${TEST_DATA.insufficientQuantity}`,
      authParams
    );

    if (response.status === 404) {
      return "failed";
    }
    if (response.status === 401 || response.status === 403) {
      return "skipped";
    }

    const parsed = parseJsonResponse(response);
    const stockInsufficient = response.status === 200 && isStockInsufficientPayload(parsed?.data);

    check(response, {
      "case04 stock insufficient path": () => stockInsufficient,
    });

    return stockInsufficient ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case05RefundFlow(data) {
  runCase("05", "refund-flow", () => {
    const authToken = getAuthTokenFromSetup(data);
    if (!authToken || !TEST_DATA.orderId || !ORDER_NO) {
      return "skipped";
    }

    const response = http.post(
      `${BASE_URL}/api/v1/refund/create`,
      REFUND_CREATE_BODY,
      jsonHeaders(authToken)
    );

    const refundCreated = isResultSuccess(response);
    check(response, {
      "case05 refund create success": () => refundCreated,
    });

    return refundCreated ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case06EventIdempotency(data) {
  runCase("06", "event-idempotency", () => {
    const authToken = getAuthTokenFromSetup(data);
    if (!authToken || !TEST_DATA.paymentId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const tolerantAuthParams = jsonHeaders(authToken, true);
    const first = http.post(
      `${BASE_URL}/api/payments/${TEST_DATA.paymentId}/success`,
      null,
      tolerantAuthParams
    );
    const second = http.post(
      `${BASE_URL}/api/payments/${TEST_DATA.paymentId}/success`,
      null,
      tolerantAuthParams
    );

    const firstOk = first.status !== 404 && first.status < 500;
    const secondOk = second.status !== 404 && second.status < 500;

    check(first, {
      "case06 first payment success call accepted": () => firstOk,
    });
    check(second, {
      "case06 second payment success call accepted": () => secondOk,
    });

    return firstOk && secondOk ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case07SearchSync(data) {
  runCase("07", "search-sync", () => {
    const authToken = CASE07_AUTH_TOKEN || getAuthTokenFromSetup(data);
    if (!CASE07_AUTH_TOKEN && !hasMerchantOrAdminRole(getAuthUserTypeFromSetup(data))) {
      return "skipped";
    }
    if (!authToken || !TEST_DATA.productId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const productResponse = http.get(`${BASE_URL}/api/product/${TEST_DATA.productId}`, authParams);
    if (!isResultSuccess(productResponse)) {
      return "failed";
    }

    const product = parseJsonResponse(productResponse)?.data;
    if (!product) {
      return "failed";
    }

    const updatePayload = {
      shopId: Number(product.shopId),
      name: `${product.name || "k6-product"}-k6`,
      price: String(product.price || "1.00"),
      stockQuantity: Number(product.stockQuantity || 1),
      categoryId: Number(product.categoryId || TEST_DATA.categoryId || 1),
      status: Number(product.status || 1),
      description: "k6 acceptance case07",
    };

    const updateResponse = http.put(
      `${BASE_URL}/api/product/${TEST_DATA.productId}`,
      JSON.stringify(updatePayload),
      authParams
    );
    if (!isResultSuccess(updateResponse)) {
      return "failed";
    }

    sleep(SEARCH_DELAY_SECONDS);

    const keyword = encodeURIComponent((product.name || "k6").split("-")[0]);
    const searchResponse = http.get(`${BASE_URL}/api/search/search?keyword=${keyword}&page=0&size=5`);
    const searchOk = searchResponse.status !== 404;

    check(searchResponse, {
      "case07 search route reachable": () => searchOk,
    });

    return searchOk ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case08RegressionBaseline() {
  runCase("08", "regression-baseline", () => {
    if (!HEALTH_BATCH_REQUESTS.length) {
      return "skipped";
    }

    const responses = http.batch(HEALTH_BATCH_REQUESTS);
    const allGood = responses.every(
      (response) => response.status >= 200 && response.status < 500 && response.status !== 404
    );

    check(responses, {
      "case08 health endpoints reachable": () => allGood,
    });

    return allGood ? "success" : "failed";
  });

  sleepBetweenCases();
}
