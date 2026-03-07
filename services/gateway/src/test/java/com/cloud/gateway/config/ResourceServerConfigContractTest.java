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

        int productIndex = source.indexOf(".pathMatchers(\"/api/product/**\", \"/api/category/**\").permitAll()");
        int searchIndex = source.indexOf(".pathMatchers(\"/api/search/**\").permitAll()");
        int apiIndex = source.indexOf(".pathMatchers(\"/api/**\").authenticated()");

        assertThat(productIndex).isGreaterThan(-1);
        assertThat(searchIndex).isGreaterThan(-1);
        assertThat(apiIndex).isGreaterThan(-1);
        assertThat(productIndex).isLessThan(apiIndex);
        assertThat(searchIndex).isLessThan(apiIndex);
        assertThat(source).doesNotContain("\"/users/**\"");
        assertThat(source).doesNotContain("\"/product/**\"");
        assertThat(source).doesNotContain("\"/payment/**\"");
        assertThat(source).doesNotContain("\"/stock/**\"");
    }
}
