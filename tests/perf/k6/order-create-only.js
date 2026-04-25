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

const SPU_ID = String(__ENV.SPU_ID || "").trim();
const SKU_ID = String(__ENV.SKU_ID || "").trim();
const AUTH_USER_ID = String(__ENV.AUTH_USER_ID || "").trim();

const orderCreateSuccessRate = new Rate("order_create_success_rate");
const orderCreateDurationMs = new Trend("order_create_duration_ms", true);

const DEFAULT_JSON_HEADERS = Object.freeze({ "Content-Type": "application/json" });
let missingPrerequisiteLogged = false;

function authParams(token, idempotencyKey) {
  return {
    headers: {
      ...DEFAULT_JSON_HEADERS,
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": idempotencyKey,
    },
    timeout: REQUEST_TIMEOUT,
  };
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

function buildClientOrderId(userId) {
  return `k6-order-only-${userId}-${__VU}-${__ITER}-${Date.now()}`;
}

function buildOrderCreateBody(userId, clientOrderId) {
  const quantity = Number(__ENV.ORDER_ITEM_QUANTITY || 1);
  const unitPrice = String(__ENV.ORDER_ITEM_PRICE || "99.99");
  const totalAmount = String(__ENV.ORDER_TOTAL_AMOUNT || unitPrice);
  const payableAmount = String(__ENV.ORDER_PAY_AMOUNT || totalAmount);
  const payload = {
    userId: Number(userId),
    spuId: Number(SPU_ID),
    skuId: Number(SKU_ID),
    quantity,
    receiverName: __ENV.RECEIVER_NAME || "k6 user",
    receiverPhone: __ENV.RECEIVER_PHONE || "13800138000",
    receiverAddress: __ENV.RECEIVER_ADDRESS || "k6 road",
    totalAmount,
    payableAmount,
    clientOrderId,
    remark: "k6 order-only",
  };
  return JSON.stringify(payload);
}

function buildIdempotencyKey(clientOrderId) {
  return clientOrderId;
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
  const setupData = {
    authToken: String(__ENV.AUTH_TOKEN || "").trim(),
    authUserId: AUTH_USER_ID,
  };
  validatePrerequisites(setupData);
  return setupData;
}

function validatePrerequisites(setupData) {
  const missing = [];
  if (!SPU_ID) missing.push("SPU_ID");
  if (!SKU_ID) missing.push("SKU_ID");
  if (!setupData?.authToken) missing.push("AUTH_TOKEN");
  if (!String(setupData?.authUserId || "").trim()) missing.push("AUTH_USER_ID");

  if (missing.length > 0) {
    throw new Error(`[order-only] missing required inputs: ${missing.join(", ")}`);
  }
}

export default function run(data) {
  const authToken = String(data?.authToken || "").trim();
  const authUserId = String(data?.authUserId || AUTH_USER_ID || "").trim();
  if (!authToken || !authUserId || !SPU_ID || !SKU_ID) {
    if (!missingPrerequisiteLogged) {
      missingPrerequisiteLogged = true;
      console.error("[order-only] skipped iteration due to missing auth or order IDs");
    }
    orderCreateSuccessRate.add(false);
    sleep(1);
    return;
  }

  const startedAt = Date.now();
  const clientOrderId = buildClientOrderId(authUserId);
  const idempotencyKey = buildIdempotencyKey(clientOrderId);
  const response = http.post(
    `${BASE_URL}/api/orders`,
    buildOrderCreateBody(authUserId, clientOrderId),
    authParams(authToken, idempotencyKey)
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
