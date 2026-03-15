package com.cloud.gateway.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceServerConfigContractTest {

    private static final Path CONFIG =
            Path.of("src/main/java/com/cloud/gateway/config/ResourceServerConfig.java");

    @Test
    void searchRoutesShouldRemainPublicBeforeGenericApiMatcher() throws IOException {
        String source = Files.readString(CONFIG);

        int searchIndex = source.indexOf(".pathMatchers(\"/api/search/**\").permitAll()");
        int productViewIndex = source.indexOf(".pathMatchers(\"/api/product/*/view\").permitAll()");
        int anyIndex = source.indexOf(".anyExchange().authenticated()");

        assertThat(searchIndex).isGreaterThan(-1);
        assertThat(productViewIndex).isGreaterThan(-1);
        assertThat(anyIndex).isGreaterThan(-1);
        assertThat(searchIndex).isLessThan(anyIndex);
        assertThat(productViewIndex).isLessThan(anyIndex);
        assertThat(source).doesNotContain("\"/users/**\"");
        assertThat(source).doesNotContain("\"/product/**\"");
        assertThat(source).doesNotContain("\"/payment/**\"");
        assertThat(source).doesNotContain("\"/stock/**\"");
    }
}
