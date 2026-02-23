import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:80";
const DEFAULT_CASE_VUS = Number(__ENV.CASE_VUS || 1);
const DEFAULT_CASE_DURATION = __ENV.CASE_DURATION || "30s";
const CASE_STAGE_SECONDS = Number(__ENV.CASE_STAGE_SECONDS || 35);
const SEARCH_DELAY_SECONDS = Number(__ENV.SEARCH_DELAY_SECONDS || 2);

const DEFAULT_HEALTH_TARGETS = [
  "http://host.docker.internal:80/actuator/health",
  "http://host.docker.internal:8081/actuator/health",
  "http://host.docker.internal:8082/actuator/health",
  "http://host.docker.internal:8083/actuator/health",
  "http://host.docker.internal:8084/actuator/health",
  "http://host.docker.internal:8085/actuator/health",
  "http://host.docker.internal:8086/actuator/health",
  "http://host.docker.internal:8087/actuator/health",
];

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

function jsonHeaders(token) {
  const headers = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return {
    headers,
    timeout: "30s",
  };
}

function parseJsonResponse(response) {
  if (!response || !response.body) {
    return null;
  }

  try {
    return JSON.parse(response.body);
  } catch (error) {
    return null;
  }
}

function isResultSuccess(response) {
  if (!response) {
    return false;
  }
  if (response.status !== 200) {
    return false;
  }

  const parsed = parseJsonResponse(response);
  return parsed && Number(parsed.code) === 200;
}

function markResult(tags, result) {
  acceptanceCase.add(1, { ...tags, result });
  acceptanceCaseSuccessRate.add(result === "success", tags);

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

function getAuthTokenFromSetup(data) {
  if (data && data.authToken) {
    return data.authToken;
  }

  return "";
}

export function setup() {
  if (__ENV.AUTH_TOKEN) {
    return { authToken: __ENV.AUTH_TOKEN };
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

  const loginResponse = http.post(
    `${BASE_URL}/auth/sessions`,
    JSON.stringify(loginPayload),
    jsonHeaders("")
  );

  if (!isResultSuccess(loginResponse)) {
    console.error(`[setup] auth login failed, status=${loginResponse.status}`);
    return { authToken: "" };
  }

  const parsed = parseJsonResponse(loginResponse);
  const accessToken = parsed?.data?.access_token;
  if (!accessToken) {
    console.error("[setup] access token missing in login response");
    return { authToken: "" };
  }

  return { authToken: accessToken };
}

export function case01GatewayRoute() {
  runCase("01", "gateway-route", () => {
    const endpoints = [
      "/api/query/users?username=admin",
      "/api/product?page=1&size=1",
      "/api/orders?page=1&size=1",
      "/api/payments?page=1&size=1",
      "/api/stocks?page=1&size=1",
      "/api/search/basic?keyword=demo&page=0&size=1",
    ];

    const responses = http.batch(
      endpoints.map((path) => ["GET", `${BASE_URL}${path}`])
    );

    const notFoundCount = responses.filter((response) => response.status === 404).length;
    const allReachable = notFoundCount === 0;

    check(responses, {
      "case01 routes are not 404": () => allReachable,
    });

    return allReachable ? "success" : "failed";
  });

  sleep(1);
}

export function case02OrderCreated(data) {
  runCase("02", "order-created", () => {
    const authToken = getAuthTokenFromSetup(data);
    const userId = getLongEnv("USER_ID");
    const shopId = getLongEnv("SHOP_ID");
    const productId = getLongEnv("PRODUCT_ID");

    if (!authToken || !userId || !shopId || !productId) {
      return "skipped";
    }

    const orderPayload = {
      userId,
      shopId,
      receiverName: __ENV.RECEIVER_NAME || "k6 user",
      receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
      receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
      payType: Number(__ENV.PAY_TYPE || 1),
      totalAmount: __ENV.ORDER_TOTAL_AMOUNT || "99.99",
      payAmount: __ENV.ORDER_PAY_AMOUNT || "99.99",
      remark: "k6 acceptance case02",
      orderItems: [
        {
          productId,
          productName: __ENV.PRODUCT_NAME || "k6-product",
          price: __ENV.ORDER_ITEM_PRICE || "99.99",
          quantity: Number(__ENV.ORDER_ITEM_QUANTITY || 1),
        },
      ],
    };

    const createOrderResponse = http.post(
      `${BASE_URL}/api/orders`,
      JSON.stringify(orderPayload),
      jsonHeaders(authToken)
    );

    const createOk = isResultSuccess(createOrderResponse);
    check(createOrderResponse, {
      "case02 create order success": () => createOk,
    });
    if (!createOk) {
      return "failed";
    }

    const orderId = parseJsonResponse(createOrderResponse)?.data?.id;
    if (!orderId) {
      return "failed";
    }

    sleep(1);

    const paymentResponse = http.get(
      `${BASE_URL}/api/payments/order/${orderId}`,
      jsonHeaders(authToken)
    );
    const stockResponse = http.get(
      `${BASE_URL}/api/stocks/product/${productId}`,
      jsonHeaders(authToken)
    );

    const linkageOk = paymentResponse.status !== 404 && stockResponse.status !== 404;
    check(paymentResponse, {
      "case02 payment route reachable": (response) => response.status !== 404,
    });
    check(stockResponse, {
      "case02 stock route reachable": (response) => response.status !== 404,
    });

    return linkageOk ? "success" : "failed";
  });

  sleep(1);
}

export function case03PaymentSuccess(data) {
  runCase("03", "payment-success", () => {
    const authToken = getAuthTokenFromSetup(data);
    let paymentId = getLongEnv("PAYMENT_ID");
    const orderId = getLongEnv("ORDER_ID");

    if (!authToken) {
      return "skipped";
    }

    if (!paymentId && orderId) {
      const paymentByOrder = http.get(
        `${BASE_URL}/api/payments/order/${orderId}`,
        jsonHeaders(authToken)
      );
      paymentId = Number(parseJsonResponse(paymentByOrder)?.data?.id || 0);
    }

    if (!paymentId) {
      return "skipped";
    }

    const paySuccessResponse = http.post(
      `${BASE_URL}/api/payments/${paymentId}/success`,
      null,
      jsonHeaders(authToken)
    );

    const paySuccessOk = paySuccessResponse.status === 200 || paySuccessResponse.status === 409;
    check(paySuccessResponse, {
      "case03 mark payment success": () => paySuccessOk,
    });

    if (orderId) {
      const orderStatusResponse = http.get(
        `${BASE_URL}/api/orders/${orderId}/paid-status`,
        jsonHeaders(authToken)
      );
      check(orderStatusResponse, {
        "case03 order paid-status route reachable": (response) => response.status !== 404,
      });
    }

    return paySuccessOk ? "success" : "failed";
  });

  sleep(1);
}

export function case04StockInsufficient() {
  runCase("04", "stock-insufficient", () => {
    const productId = getLongEnv("PRODUCT_ID");
    if (!productId) {
      return "skipped";
    }

    const pressureQty = Number(__ENV.INSUFFICIENT_QUANTITY || 999999);
    const response = http.get(`${BASE_URL}/api/stocks/check/${productId}/${pressureQty}`);

    if (response.status === 404) {
      return "failed";
    }

    const parsed = parseJsonResponse(response);
    const data = parsed?.data;
    const stockInsufficient = response.status === 200 && data === false;

    check(response, {
      "case04 stock insufficient path": () => stockInsufficient,
    });

    return stockInsufficient ? "success" : "failed";
  });

  sleep(1);
}

export function case05RefundFlow(data) {
  runCase("05", "refund-flow", () => {
    const authToken = getAuthTokenFromSetup(data);
    const orderId = getLongEnv("ORDER_ID");
    const orderNo = __ENV.ORDER_NO;

    if (!authToken || !orderId || !orderNo) {
      return "skipped";
    }

    const payload = {
      orderId,
      orderNo,
      refundType: Number(__ENV.REFUND_TYPE || 1),
      refundReason: __ENV.REFUND_REASON || "k6 refund",
      refundDescription: __ENV.REFUND_DESCRIPTION || "k6 acceptance case05",
      refundAmount: __ENV.REFUND_AMOUNT || "1.00",
      refundQuantity: Number(__ENV.REFUND_QUANTITY || 1),
    };

    const response = http.post(
      `${BASE_URL}/api/v1/refund/create`,
      JSON.stringify(payload),
      jsonHeaders(authToken)
    );

    const refundCreated = isResultSuccess(response);
    check(response, {
      "case05 refund create success": () => refundCreated,
    });

    return refundCreated ? "success" : "failed";
  });

  sleep(1);
}

export function case06EventIdempotency(data) {
  runCase("06", "event-idempotency", () => {
    const authToken = getAuthTokenFromSetup(data);
    const paymentId = getLongEnv("PAYMENT_ID");

    if (!authToken || !paymentId) {
      return "skipped";
    }

    const first = http.post(
      `${BASE_URL}/api/payments/${paymentId}/success`,
      null,
      jsonHeaders(authToken)
    );
    const second = http.post(
      `${BASE_URL}/api/payments/${paymentId}/success`,
      null,
      jsonHeaders(authToken)
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

  sleep(1);
}

export function case07SearchSync(data) {
  runCase("07", "search-sync", () => {
    const authToken = getAuthTokenFromSetup(data);
    const productId = getLongEnv("PRODUCT_ID");

    if (!authToken || !productId) {
      return "skipped";
    }

    const productResponse = http.get(
      `${BASE_URL}/api/product/${productId}`,
      jsonHeaders(authToken)
    );

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
      categoryId: Number(product.categoryId || __ENV.CATEGORY_ID || 1),
      status: Number(product.status || 1),
      description: "k6 acceptance case07",
    };

    const updateResponse = http.put(
      `${BASE_URL}/api/product/${productId}`,
      JSON.stringify(updatePayload),
      jsonHeaders(authToken)
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

  sleep(1);
}

export function case08RegressionBaseline() {
  runCase("08", "regression-baseline", () => {
    const healthTargets = (__ENV.HEALTH_TARGETS || DEFAULT_HEALTH_TARGETS.join(","))
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item.length > 0);

    if (!healthTargets.length) {
      return "skipped";
    }

    const responses = http.batch(healthTargets.map((target) => ["GET", target]));
    const allGood = responses.every((response) => response.status >= 200 && response.status < 500 && response.status !== 404);

    check(responses, {
      "case08 health endpoints reachable": () => allGood,
    });

    return allGood ? "success" : "failed";
  });

  sleep(1);
}
