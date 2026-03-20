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
    int anyIndex = normalized.indexOf(".anyExchange().authenticated()");

    assertThat(searchIndex).isGreaterThan(-1);
    assertThat(productViewIndex).isGreaterThan(-1);
    assertThat(websocketIndex).isGreaterThan(-1);
    assertThat(anyIndex).isGreaterThan(-1);
    assertThat(searchIndex).isLessThan(anyIndex);
    assertThat(productViewIndex).isLessThan(anyIndex);
    assertThat(websocketIndex).isLessThan(anyIndex);
    assertThat(source).doesNotContain(".pathMatchers(\"/ws/**\").permitAll()");
    assertThat(source).doesNotContain("\"/users/**\"");
    assertThat(source).doesNotContain("\"/product/**\"");
    assertThat(source).doesNotContain("\"/payment/**\"");
    assertThat(source).doesNotContain("\"/stock/**\"");
  }
}
