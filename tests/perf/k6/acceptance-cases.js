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
const DEFAULT_QUERY_USERNAME = String(
  __ENV.QUERY_USERNAME || __ENV.ADMIN_USERNAME || __ENV.USERNAME || "t_admin_24001"
).trim();
const DEFAULT_PAYMENT_NO = String(__ENV.PAYMENT_NO || "PAY202603050001").trim();
const DEFAULT_STOCK_SKU_ID = String(__ENV.SKU_ID || "51001").trim();
const DEFAULT_SEARCH_KEYWORD = String(__ENV.SEARCH_KEYWORD || "demo").trim();

const GATEWAY_BATCH_REQUESTS = [
  `/api/admin/users?username=${encodeURIComponent(DEFAULT_QUERY_USERNAME || "t_admin_24001")}`,
  "/api/categories/tree?enabledOnly=true",
  "/api/orders",
  `/api/payment-orders/${encodeURIComponent(DEFAULT_PAYMENT_NO || "PAY202603050001")}`,
  `/api/admin/stocks/ledger/${encodeURIComponent(DEFAULT_STOCK_SKU_ID || "51001")}`,
  `/api/search/products?keyword=${encodeURIComponent(DEFAULT_SEARCH_KEYWORD || "demo")}&page=0&size=1`,
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

function buildClientOrderId(prefix = "k6-order") {
  return `${prefix}-${__VU}-${__ITER}-${Date.now()}`;
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
  merchantId: getIdEnv("MERCHANT_ID"),
  spuId: getIdEnv("SPU_ID", getIdEnv("PRODUCT_ID")),
  skuId: getIdEnv("SKU_ID"),
  categoryId: getIdEnv("CATEGORY_ID", "1"),
  mainOrderId: getIdEnv("MAIN_ORDER_ID", getIdEnv("ORDER_ID")),
  paymentNo: String(__ENV.PAYMENT_NO || "").trim(),
  refundNo: String(__ENV.REFUND_NO || "").trim(),
  afterSaleNo: String(__ENV.AFTER_SALE_NO || "").trim(),
  insufficientQuantity: Number(__ENV.INSUFFICIENT_QUANTITY || 999999),
});
const PAYMENT_INTERNAL_TOKEN = String(__ENV.PAYMENT_INTERNAL_TOKEN || "").trim();
const CASE03_AUTH_TOKEN = String(__ENV.CASE03_AUTH_TOKEN || "").trim();
const CASE04_AUTH_TOKEN = String(__ENV.CASE04_AUTH_TOKEN || "").trim();
const CASE05_AUTH_TOKEN = String(__ENV.CASE05_AUTH_TOKEN || "").trim();
const CASE06_AUTH_TOKEN = String(__ENV.CASE06_AUTH_TOKEN || "").trim();
const CASE07_AUTH_TOKEN = String(__ENV.CASE07_AUTH_TOKEN || "").trim();
const AUTH_USER_ID = getIdEnv("AUTH_USER_ID");
const AUTH_PRIMARY_ROLE_ENV = String(__ENV.AUTH_PRIMARY_ROLE || "USER").toUpperCase();

function buildOrderCreateBody(userId, clientOrderId) {
  const skuId = Number(TEST_DATA.skuId || __ENV.ORDER_SKU_ID || 0);
  const spuId = Number(TEST_DATA.spuId || __ENV.ORDER_SPU_ID || 0);
  const quantity = Number(__ENV.ORDER_ITEM_QUANTITY || 1);
  const unitPrice = String(__ENV.ORDER_ITEM_PRICE || "99.99");
  const totalAmount = String(__ENV.ORDER_TOTAL_AMOUNT || unitPrice);
  const payableAmount = String(__ENV.ORDER_PAY_AMOUNT || totalAmount);

  const payload = {
    userId: userId ? Number(userId) : undefined,
    spuId,
    skuId,
    quantity,
    totalAmount,
    payableAmount,
    clientOrderId,
    remark: "k6 acceptance case02",
    receiverName: __ENV.RECEIVER_NAME || "k6 user",
    receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
    receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
  };

  return JSON.stringify(payload);
}

const REFUND_CREATE_BODY = JSON.stringify({
  refundNo: TEST_DATA.refundNo || `k6-refund-${Date.now()}`,
  paymentNo: TEST_DATA.paymentNo || "k6-payment-no",
  afterSaleNo: TEST_DATA.afterSaleNo || `k6-after-sale-${Date.now()}`,
  refundAmount: __ENV.REFUND_AMOUNT || "1.00",
  reason: __ENV.REFUND_REASON || "k6 refund",
  idempotencyKey: __ENV.REFUND_IDEMPOTENCY_KEY || `k6-refund-key-${Date.now()}`,
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

function isResultEnvelope(response) {
  const parsed = parseJsonResponse(response);
  return !!parsed && Object.prototype.hasOwnProperty.call(parsed, "code")
    && Object.prototype.hasOwnProperty.call(parsed, "message")
    && Object.prototype.hasOwnProperty.call(parsed, "data");
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

function getAuthPrimaryRoleFromSetup(data) {
  const setupPrimaryRole = String(data?.authPrimaryRole || "").toUpperCase();
  if (setupPrimaryRole) {
    return setupPrimaryRole;
  }
  return AUTH_PRIMARY_ROLE_ENV;
}

function hasMerchantOrAdminRole(role) {
  return role === "MERCHANT" || role === "ADMIN";
}

function getInternalFlowToken(data, explicitToken = "") {
  const directToken = String(explicitToken || PAYMENT_INTERNAL_TOKEN || "").trim();
  if (directToken) {
    return directToken;
  }

  const primaryRole = getAuthPrimaryRoleFromSetup(data);
  if (primaryRole === "SERVICE" || primaryRole === "INTERNAL") {
    return getAuthTokenFromSetup(data);
  }

  return "";
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

function isStockInsufficientResponse(response) {
  if (!response) {
    return false;
  }

  const parsed = parseJsonResponse(response);
  const code = Number(parsed?.code);
  const message = String(parsed?.message || "");
  const messageMatch = /insufficient|stock|库存|缺货|不足/i.test(message);

  if (response.status === 200 && isResultEnvelope(response)) {
    return isStockInsufficientPayload(parsed?.data);
  }

  if (response.status === 409) {
    return code === 6002 || messageMatch;
  }

  if (response.status === 400) {
    return code === 6002 || code === 502 || messageMatch;
  }

  return false;
}


function extractDataIdFromResponse(response) {
  const parsed = parseJsonResponse(response);
  if (typeof parsed?.data === "number") {
    return String(parsed.data);
  }
  const dataId = parsed?.data?.id;
  if (typeof dataId === "number" || typeof dataId === "string") {
    return String(dataId);
  }

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
  return {
    authToken: String(__ENV.AUTH_TOKEN || "").trim(),
    authUserId: AUTH_USER_ID || TEST_DATA.userId || "",
    authPrimaryRole: AUTH_PRIMARY_ROLE_ENV,
  };
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
    if (!authToken || !authUserId || !TEST_DATA.spuId || !TEST_DATA.skuId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken, true);
    const clientOrderId = buildClientOrderId();
    const createOrderBody = buildOrderCreateBody(authUserId, clientOrderId);
    const createOrderResponse = http.post(`${BASE_URL}/api/orders`, createOrderBody, {
      ...authParams,
      headers: {
        ...authParams.headers,
        "Idempotency-Key": clientOrderId,
      },
    });
    const createOk = isResultSuccess(createOrderResponse) && isResultEnvelope(createOrderResponse);
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

    const orderId = extractDataIdFromResponse(createOrderResponse)
      || String(parseJsonResponse(createOrderResponse)?.data?.mainOrder?.id || "");
    if (!orderId || !/^\d+$/.test(orderId)) {
      return "failed";
    }

    const authReachableParams = jsonHeaders(authToken, true);
    const [orderResponse, stockResponse] = http.batch([
      ["GET", `${BASE_URL}/api/orders/${orderId}`, null, authReachableParams],
      ["GET", `${BASE_URL}/api/admin/stocks/ledger/${TEST_DATA.skuId}`, null, authReachableParams],
    ]);

    const orderReachable = orderResponse.status !== 404 && isResultEnvelope(orderResponse);
    const stockReachable = stockResponse.status !== 404 && isResultEnvelope(stockResponse);
    const linkageOk = orderReachable && stockReachable;

    check(orderResponse, {
      "case02 order aggregate reachable": () => orderReachable,
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
    const authToken = getInternalFlowToken(data, CASE03_AUTH_TOKEN);
    const paymentNo = TEST_DATA.paymentNo;

    if (!authToken || !paymentNo) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken, true);
    const tolerantAuthParams = jsonHeaders(authToken, true);
    const paySuccessResponse = http.get(
      `${BASE_URL}/api/payment-orders/${encodeURIComponent(paymentNo)}`,
      authParams
    );
    const paySuccessOk = paySuccessResponse.status !== 404 && isResultEnvelope(paySuccessResponse);
    check(paySuccessResponse, {
      "case03 payment query route reachable": () => paySuccessOk,
    });

    const checkoutResponse = http.post(
      `${BASE_URL}/api/payment-orders/${encodeURIComponent(paymentNo)}/checkout-sessions`,
      null,
      tolerantAuthParams
    );
    const checkoutOk = checkoutResponse.status !== 404 && isResultEnvelope(checkoutResponse);
    check(checkoutResponse, {
      "case03 checkout-session route reachable": () => checkoutOk,
    });

    return paySuccessOk && checkoutOk ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case04StockInsufficient(data) {
  runCase("04", "stock-insufficient", () => {
    const authToken = CASE04_AUTH_TOKEN || getAuthTokenFromSetup(data);
    const authUserId = getAuthUserIdFromSetup(data);
    if (!authToken || !TEST_DATA.spuId || !TEST_DATA.skuId) {
      return "skipped";
    }

    const unitPrice = String(__ENV.ORDER_ITEM_PRICE || "99.99");
    const quantity = Number(TEST_DATA.insufficientQuantity || 999999);
    const totalAmount = (Number(unitPrice) * quantity).toFixed(2);
    const response = http.post(
      `${BASE_URL}/api/orders`,
      JSON.stringify({
        userId: authUserId ? Number(authUserId) : undefined,
        spuId: Number(TEST_DATA.spuId),
        skuId: Number(TEST_DATA.skuId),
        quantity,
        totalAmount,
        payableAmount: totalAmount,
        clientOrderId: `k6-insufficient-${Date.now()}`,
        remark: "k6 stock insufficient check",
        receiverName: __ENV.RECEIVER_NAME || "k6 user",
        receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
        receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
      }),
      {
        ...jsonHeaders(authToken, true),
        headers: {
          ...jsonHeaders(authToken, true).headers,
          "Idempotency-Key": `k6-insufficient-${__VU}-${__ITER}-${Date.now()}`,
        },
      }
    );

    if (response.status === 404) {
      return "failed";
    }
    if (response.status === 401 || response.status === 403) {
      return "skipped";
    }

    const stockInsufficient = isStockInsufficientResponse(response);

    check(response, {
      "case04 stock insufficient path": () => stockInsufficient,
    });

    return stockInsufficient ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case05RefundFlow(data) {
  runCase("05", "refund-flow", () => {
    const authToken = getInternalFlowToken(data, CASE05_AUTH_TOKEN);
    if (!authToken || !TEST_DATA.paymentNo) {
      return "skipped";
    }

    const response = http.post(
      `${BASE_URL}/api/payment-refunds`,
      REFUND_CREATE_BODY,
      jsonHeaders(authToken, true)
    );

    const refundCreated = response.status !== 404 && isResultEnvelope(response);
    check(response, {
      "case05 refund route reachable": () => refundCreated,
    });

    return refundCreated ? "success" : "failed";
  });

  sleepBetweenCases();
}

export function case06EventIdempotency(data) {
  runCase("06", "event-idempotency", () => {
    const authToken = getInternalFlowToken(data, CASE06_AUTH_TOKEN);
    if (!authToken || !TEST_DATA.paymentNo) {
      return "skipped";
    }

    const tolerantAuthParams = jsonHeaders(authToken, true);
    const first = http.post(
      `${BASE_URL}/api/payment-orders/${encodeURIComponent(TEST_DATA.paymentNo)}/checkout-sessions`,
      null,
      tolerantAuthParams
    );
    const second = http.post(
      `${BASE_URL}/api/payment-orders/${encodeURIComponent(TEST_DATA.paymentNo)}/checkout-sessions`,
      null,
      tolerantAuthParams
    );

    const firstOk = first.status !== 404 && first.status < 500 && isResultEnvelope(first);
    const secondOk = second.status !== 404 && second.status < 500 && isResultEnvelope(second);

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
    if (!CASE07_AUTH_TOKEN && !hasMerchantOrAdminRole(getAuthPrimaryRoleFromSetup(data))) {
      return "skipped";
    }
    if (!authToken || !TEST_DATA.spuId || !TEST_DATA.categoryId) {
      return "skipped";
    }

    const authParams = jsonHeaders(authToken);
    const productResponse = http.get(`${BASE_URL}/api/spus/${TEST_DATA.spuId}`, authParams);
    if (!isResultSuccess(productResponse)) {
      return "failed";
    }

    const product = parseJsonResponse(productResponse)?.data;
    if (!product) {
      return "failed";
    }

    const updatePayload = {
      spu: {
        categoryId: Number(product.categoryId || TEST_DATA.categoryId || 1),
        spuName: `${product.spuName || product.name || "k6-product"}-k6`,
        brandId: Number(product.brandId || __ENV.PRODUCT_BRAND_ID || 1),
        merchantId: Number(product.merchantId || TEST_DATA.merchantId || __ENV.ORDER_MERCHANT_ID || 0),
        status: Number(product.status || 1),
        subtitle: product.subtitle || "k6 subtitle",
        description: "k6 acceptance case07",
        mainImage: product.mainImage || "",
      },
      skus: (Array.isArray(product.skus) ? product.skus : [])
        .slice(0, 1)
        .map((sku) => ({
          skuId: Number(sku.skuId || TEST_DATA.skuId || 0),
          skuCode: sku.skuCode || __ENV.ORDER_SKU_CODE || `SKU-${TEST_DATA.skuId || 0}`,
          skuName: `${sku.skuName || __ENV.ORDER_SKU_NAME || "k6-sku"}-k6`,
          specJson: sku.specJson || "{}",
          salePrice: String(sku.salePrice || __ENV.ORDER_ITEM_PRICE || "99.99"),
          marketPrice: String(sku.marketPrice || __ENV.ORDER_ITEM_PRICE || "99.99"),
          costPrice: String(sku.costPrice || __ENV.ORDER_ITEM_PRICE || "99.99"),
          status: Number(sku.status || 1),
          imageUrl: sku.imageUrl || "",
        })),
    };
    if (!updatePayload.spu.merchantId || !updatePayload.skus.length) {
      return "skipped";
    }

    const updateResponse = http.put(
      `${BASE_URL}/api/spus/${TEST_DATA.spuId}`,
      JSON.stringify(updatePayload),
      authParams
    );
    if (!isResultSuccess(updateResponse)) {
      return "failed";
    }

    sleep(SEARCH_DELAY_SECONDS);

    const keyword = encodeURIComponent((product.spuName || product.name || "k6").split("-")[0]);
    const searchResponse = http.get(
      `${BASE_URL}/api/search/products?keyword=${keyword}&page=0&size=5`
    );
    const searchOk = searchResponse.status !== 404 && isResultEnvelope(searchResponse);

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
