package com.cloud.gateway.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class MainFlowE2ETest {

  @BeforeAll
  static void init() {
    E2ETestSupport.initRestAssured();
  }

  @Test
  void shouldCreateOrderAndQueryMainOrder() {
    E2ETestSupport.assumeHasAccessToken();

    OrderContext order = createOrder(E2ETestSupport.accessToken());

    Response queryResponse =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
            .when()
            .get("/api/orders/" + order.mainOrderId);

    assertThat(queryResponse.statusCode()).isEqualTo(200);
    JsonPath queryJson = queryResponse.jsonPath();
    assertThat(queryJson.getInt("code")).isEqualTo(200);
    assertThat(queryJson.getLong("data.id")).isEqualTo(order.mainOrderId);
    assertThat(queryJson.getString("data.orderNo")).isEqualTo(order.mainOrderNo);
  }

  @Test
  void shouldCreatePaymentAndCallback() {
    E2ETestSupport.assumeHasAccessToken();
    E2ETestSupport.assumeHasInternalToken();

    OrderContext order = createOrder(E2ETestSupport.accessToken());
    String paymentNo = E2ETestSupport.newPaymentNo();

    Map<String, Object> paymentBody = new HashMap<>();
    paymentBody.put("paymentNo", paymentNo);
    paymentBody.put("mainOrderNo", order.mainOrderNo);
    paymentBody.put("subOrderNo", order.subOrderNo);
    paymentBody.put("userId", E2ETestSupport.userId());
    paymentBody.put("amount", new BigDecimal("4999.00"));
    paymentBody.put("channel", "ALIPAY");
    paymentBody.put("idempotencyKey", E2ETestSupport.newIdempotencyKey());

    Response createPayment =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.internalToken())
            .contentType(ContentType.JSON)
            .body(paymentBody)
            .when()
            .post("/api/payments/orders");

    assertThat(createPayment.statusCode()).isEqualTo(200);
    assertThat(createPayment.jsonPath().getInt("code")).isEqualTo(200);

    Map<String, Object> callbackBody = new HashMap<>();
    callbackBody.put("paymentNo", paymentNo);
    callbackBody.put("callbackNo", E2ETestSupport.newCallbackNo());
    callbackBody.put("callbackStatus", "SUCCESS");
    callbackBody.put("providerTxnNo", "ALI-" + System.currentTimeMillis());
    callbackBody.put("idempotencyKey", E2ETestSupport.newIdempotencyKey());
    callbackBody.put("payload", "{\"tradeStatus\":\"TRADE_SUCCESS\"}");

    Response callback =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.internalToken())
            .contentType(ContentType.JSON)
            .body(callbackBody)
            .when()
            .post("/api/payments/callbacks");

    assertThat(callback.statusCode()).isEqualTo(200);
    assertThat(callback.jsonPath().getInt("code")).isEqualTo(200);

    Response queryPayment =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
            .when()
            .get("/api/payments/orders/" + paymentNo);

    assertThat(queryPayment.statusCode()).isEqualTo(200);
    assertThat(queryPayment.jsonPath().getInt("code")).isEqualTo(200);
    assertThat(queryPayment.jsonPath().getString("data.paymentNo")).isEqualTo(paymentNo);
  }

  private OrderContext createOrder(String token) {
    Map<String, Object> body = new HashMap<>();
    body.put("userId", E2ETestSupport.userId());
    body.put("spuId", E2ETestSupport.spuId());
    body.put("skuId", E2ETestSupport.skuId());
    body.put("quantity", 1);
    body.put("remark", "e2e order");
    body.put("receiverName", E2ETestSupport.receiverName());
    body.put("receiverPhone", E2ETestSupport.receiverPhone());
    body.put("receiverAddress", E2ETestSupport.receiverAddress());

    Response response =
        given()
            .header("Authorization", "Bearer " + token)
            .header("Idempotency-Key", E2ETestSupport.newIdempotencyKey())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/orders");

    assertThat(response.statusCode()).isEqualTo(200);
    JsonPath json = response.jsonPath();
    assertThat(json.getInt("code")).isEqualTo(200);

    Long mainOrderId = json.getLong("data.mainOrder.id");
    String mainOrderNo = json.getString("data.mainOrder.mainOrderNo");
    Long subOrderId = json.getLong("data.subOrders[0].subOrder.id");
    String subOrderNo = json.getString("data.subOrders[0].subOrder.subOrderNo");

    assertThat(mainOrderId).isNotNull();
    assertThat(mainOrderNo).isNotBlank();
    assertThat(subOrderId).isNotNull();
    assertThat(subOrderNo).isNotBlank();

    return new OrderContext(mainOrderId, mainOrderNo, subOrderId, subOrderNo);
  }

  private record OrderContext(
      Long mainOrderId, String mainOrderNo, Long subOrderId, String subOrderNo) {}
}
