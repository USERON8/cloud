import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = String(__ENV.K6_BASE_URL || __ENV.BASE_URL || "http://host.docker.internal:18080")
  .trim()
  .replace(/\/+$/, "");
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "30s";
const ORDER_VUS = Number(__ENV.ORDER_VUS || 8);
const ORDER_DURATION = __ENV.ORDER_DURATION || "30s";
const ORDER_SLEEP_SECONDS = Number(__ENV.ORDER_SLEEP_SECONDS || 0);
const LOGIN_RETRIES = Number(__ENV.AUTH_LOGIN_MAX_RETRIES || 3);
const LOGIN_RETRY_SLEEP_SECONDS = Number(__ENV.AUTH_LOGIN_RETRY_SLEEP_SECONDS || 1);

const SHOP_ID = String(__ENV.SHOP_ID || "").trim();
const ADDRESS_ID = String(__ENV.ADDRESS_ID || "1").trim();
const PRODUCT_ID = String(__ENV.PRODUCT_ID || "").trim();
const USER_ID = String(__ENV.USER_ID || "").trim();

const orderCreateSuccessRate = new Rate("order_create_success_rate");
const orderCreateDurationMs = new Trend("order_create_duration_ms", true);

const DEFAULT_JSON_HEADERS = Object.freeze({ "Content-Type": "application/json" });
const REQUEST_PARAMS = Object.freeze({
  headers: DEFAULT_JSON_HEADERS,
  timeout: REQUEST_TIMEOUT,
});
const authParamsCache = new Map();
let missingPrerequisiteLogged = false;

function authParams(token = "") {
  if (!token) {
    return REQUEST_PARAMS;
  }

  const cached = authParamsCache.get(token);
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
  authParamsCache.set(token, params);
  return params;
}

function parseJson(response) {
  try {
    return response ? response.json() : null;
  } catch (error) {
    return null;
  }
}

function isSuccess(response) {
  if (!response || response.status !== 200) {
    return false;
  }
  const parsed = parseJson(response);
  return parsed && Number(parsed.code) === 200;
}

function shouldRetryLogin(response) {
  if (!response) {
    return true;
  }
  return response.status === 0 || response.status === 429 || response.status >= 500;
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

function buildOrderCreateBody(userId) {
  const payload = {
    shopId: SHOP_ID,
    addressId: ADDRESS_ID,
    receiverName: __ENV.RECEIVER_NAME || "k6 user",
    receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
    receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
    payType: Number(__ENV.PAY_TYPE || 1),
    totalAmount: __ENV.ORDER_TOTAL_AMOUNT || "99.99",
    payAmount: __ENV.ORDER_PAY_AMOUNT || "99.99",
    remark: "k6 order-only",
    orderItems: [
      {
        productId: PRODUCT_ID,
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

export const options = {
  scenarios: {
    order_create_only: {
      executor: "constant-vus",
      vus: ORDER_VUS,
      duration: ORDER_DURATION,
      gracefulStop: "5s",
    },
  },
  thresholds: {
    order_create_success_rate: ["rate>=0.95"],
    order_create_duration_ms: ["p(95)<5000"],
    http_req_failed: ["rate<=0.05"],
  },
};

export function setup() {
  if (__ENV.AUTH_TOKEN) {
    const setupData = {
      authToken: __ENV.AUTH_TOKEN,
      authUserId: String(__ENV.AUTH_USER_ID || USER_ID || "").trim(),
    };
    validatePrerequisites(setupData);
    return setupData;
  }

  const username = __ENV.AUTH_USERNAME;
  const password = __ENV.AUTH_PASSWORD;
  if (!username || !password) {
    const setupData = { authToken: "", authUserId: USER_ID };
    validatePrerequisites(setupData);
    return setupData;
  }

  const loginBody = JSON.stringify({
    username,
    password,
    userType: __ENV.AUTH_USER_TYPE || "USER",
  });

  for (let attempt = 1; attempt <= LOGIN_RETRIES; attempt += 1) {
    const loginResp = http.post(`${BASE_URL}/auth/sessions`, loginBody, REQUEST_PARAMS);
    if (isSuccess(loginResp)) {
      const parsed = parseJson(loginResp);
      const authToken = parsed?.data?.access_token || parsed?.data?.accessToken || parsed?.data?.token || "";
      const authUserId = extractUserIdFromLoginResponse(loginResp) || USER_ID;
      const setupData = { authToken, authUserId };
      validatePrerequisites(setupData);
      return setupData;
    }

    if (attempt < LOGIN_RETRIES && shouldRetryLogin(loginResp)) {
      if (LOGIN_RETRY_SLEEP_SECONDS > 0) {
        sleep(LOGIN_RETRY_SLEEP_SECONDS);
      }
      continue;
    }
    break;
  }

  const setupData = { authToken: "", authUserId: USER_ID };
  validatePrerequisites(setupData);
  return setupData;
}

function validatePrerequisites(setupData) {
  const missing = [];
  if (!SHOP_ID) missing.push("SHOP_ID");
  if (!ADDRESS_ID) missing.push("ADDRESS_ID");
  if (!PRODUCT_ID) missing.push("PRODUCT_ID");
  if (!setupData?.authToken) missing.push("AUTH_TOKEN or AUTH_USERNAME/AUTH_PASSWORD");
  if (!String(setupData?.authUserId || "").trim()) missing.push("AUTH_USER_ID or login response user id");

  if (missing.length > 0) {
    throw new Error(`[order-only] missing required inputs: ${missing.join(", ")}`);
  }
}

export default function run(data) {
  const authToken = String(data?.authToken || "").trim();
  const authUserId = String(data?.authUserId || USER_ID || "").trim();
  if (!authToken || !authUserId || !SHOP_ID || !ADDRESS_ID || !PRODUCT_ID) {
    if (!missingPrerequisiteLogged) {
      missingPrerequisiteLogged = true;
      console.error("[order-only] skipped iteration due to missing auth or business IDs");
    }
    orderCreateSuccessRate.add(false);
    sleep(1);
    return;
  }

  const startedAt = Date.now();
  const response = http.post(
    `${BASE_URL}/api/orders`,
    buildOrderCreateBody(authUserId),
    authParams(authToken)
  );
  const ok = isSuccess(response);
  orderCreateDurationMs.add(Date.now() - startedAt);
  orderCreateSuccessRate.add(ok);

  check(response, {
    "order create success": () => ok,
  });

  if (!ok && __ENV.DEBUG_ORDER_ONLY === "1") {
    const body = String(response?.body || "").slice(0, 300);
    console.error(`[order-only] failed, status=${response?.status}, body=${body}`);
  }

  if (ORDER_SLEEP_SECONDS > 0) {
    sleep(ORDER_SLEEP_SECONDS);
  }
}
