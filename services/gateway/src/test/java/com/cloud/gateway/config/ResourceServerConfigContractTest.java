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

        int searchIndex = source.indexOf(".pathMatchers(\"/search/**\", \"/api/search/**\").permitAll()");
        int apiIndex = source.indexOf(".pathMatchers(\"/api/**\").authenticated()");

        assertThat(searchIndex).isGreaterThan(-1);
        assertThat(apiIndex).isGreaterThan(-1);
        assertThat(searchIndex).isLessThan(apiIndex);
    }
}
