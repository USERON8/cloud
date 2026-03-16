package com.cloud.gateway.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class ProductFlowE2ETest {

  @BeforeAll
  static void init() {
    E2ETestSupport.initRestAssured();
  }

  @Test
  void shouldFetchProductAndSkuBatch() {
    E2ETestSupport.assumeHasAccessToken();

    Response spuResponse =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
            .when()
            .get("/api/product/spu/" + E2ETestSupport.spuId());

    assertThat(spuResponse.statusCode()).isEqualTo(200);
    assertThat(spuResponse.jsonPath().getInt("code")).isEqualTo(200);

    Response categoryResponse =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
            .when()
            .get("/api/product/spu/category/" + E2ETestSupport.categoryId() + "?status=1");

    assertThat(categoryResponse.statusCode()).isEqualTo(200);
    assertThat(categoryResponse.jsonPath().getInt("code")).isEqualTo(200);

    Response skuBatchResponse =
        given()
            .header("Authorization", "Bearer " + E2ETestSupport.accessToken())
            .when()
            .get("/api/product/sku/batch?skuIds=" + E2ETestSupport.skuId());

    assertThat(skuBatchResponse.statusCode()).isEqualTo(200);
    assertThat(skuBatchResponse.jsonPath().getInt("code")).isEqualTo(200);
  }
}
