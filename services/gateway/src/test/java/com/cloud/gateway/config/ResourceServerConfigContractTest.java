package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResourceServerConfigContractTest {

  private static final Path CONFIG =
      Path.of("src/main/java/com/cloud/gateway/config/ResourceServerConfig.java");

  @Test
  void searchRoutesShouldRemainPublicBeforeGenericApiMatcher() throws IOException {
    String source = Files.readString(CONFIG);
    String normalized = source.replaceAll("\\s+", "");

    int searchIndex = normalized.indexOf(".pathMatchers(\"/api/search/**\").permitAll()");
    int productViewIndex = normalized.indexOf(".pathMatchers(\"/api/product/*/view\").permitAll()");
    int websocketIndex = normalized.indexOf(".pathMatchers(\"/ws/**\").authenticated()");
    int paymentCheckoutIndex =
        normalized.indexOf(
            ".pathMatchers(HttpMethod.GET,\"/api/payments/checkout/**\").permitAll()");
    int paymentIndex =
        normalized.indexOf(
            ".pathMatchers(\"/api/payments/**\").hasAnyRole(\"USER\",\"MERCHANT\",\"ADMIN\")");
    int userNotificationIndex =
        normalized.indexOf(
            ".pathMatchers(\"/api/user/notification/**\").hasAuthority(\"admin:all\")");
    int userProfileIndex =
        normalized.indexOf(
            ".pathMatchers(\"/api/user/profile/**\",\"/api/user/address/**\").authenticated()");
    int stockLedgerIndex =
        normalized.indexOf(
            ".pathMatchers(HttpMethod.GET,\"/api/stocks/ledger/**\").hasRole(\"ADMIN\")");
    int stockMutationIndex =
        normalized.indexOf(".pathMatchers(\"/api/stocks/**\").hasAuthority(\"SCOPE_internal\")");
    int anyIndex = normalized.indexOf(".anyExchange().authenticated()");

    assertThat(searchIndex).isGreaterThan(-1);
    assertThat(productViewIndex).isGreaterThan(-1);
    assertThat(websocketIndex).isGreaterThan(-1);
    assertThat(paymentCheckoutIndex).isGreaterThan(-1);
    assertThat(paymentIndex).isGreaterThan(-1);
    assertThat(userNotificationIndex).isGreaterThan(-1);
    assertThat(userProfileIndex).isGreaterThan(-1);
    assertThat(stockLedgerIndex).isGreaterThan(-1);
    assertThat(stockMutationIndex).isGreaterThan(-1);
    assertThat(anyIndex).isGreaterThan(-1);
    assertThat(searchIndex).isLessThan(anyIndex);
    assertThat(productViewIndex).isLessThan(anyIndex);
    assertThat(websocketIndex).isLessThan(anyIndex);
    assertThat(paymentCheckoutIndex).isLessThan(paymentIndex);
    assertThat(userNotificationIndex).isLessThan(userProfileIndex);
    assertThat(userProfileIndex).isLessThan(anyIndex);
    assertThat(stockLedgerIndex).isLessThan(stockMutationIndex);
    assertThat(stockMutationIndex).isLessThan(anyIndex);
    assertThat(source).doesNotContain(".pathMatchers(\"/ws/**\").permitAll()");
    assertThat(source).doesNotContain("\"/users/**\"");
    assertThat(source).doesNotContain("\"/product/**\"");
    assertThat(source).doesNotContain("\"/payment/**\"");
    assertThat(source).doesNotContain("\"/stock/**\"");
    assertThat(source).doesNotContain(".pathMatchers(\"/api/user/**\").hasRole(\"USER\")");
  }
}
