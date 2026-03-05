package com.cloud.gateway.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteContractTest {

    private static final Path ROUTE_CONFIG = Path.of("src/main/resources/application-route.yml");

    @Test
    void shouldRouteV2ApisToCorrectServiceAndAvoidCatchAllV2Pattern() throws IOException {
        String yaml = Files.readString(ROUTE_CONFIG);

        assertThat(yaml).contains("- id: product-service-api-v2");
        assertThat(yaml).contains("Path=/api/v2/products/**");

        assertThat(yaml).contains("- id: order-service-api-v2");
        assertThat(yaml).contains("Path=/api/orders/**,/api/v1/refund/**");
        assertThat(yaml).doesNotContain("Path=/api/v2/**");

        assertThat(yaml).contains("- id: payment-service-api-v2");
        assertThat(yaml).contains("Path=/api/v2/payments/**,/api/payments/**,/api/v1/payment/alipay/**");

        assertThat(yaml).contains("- id: stock-service-api-v2");
        assertThat(yaml).contains("Path=/api/v2/stocks/**,/api/stocks/**");
    }
}
