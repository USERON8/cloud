package com.cloud.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResourceServerConfigContractTest {

  private static final Path CONFIG =
      Path.of("src/main/java/com/cloud/user/config/ResourceServerConfig.java");

  @Test
  void userEndpointsShouldSeparateAdminNotificationsFromProfileAccess() throws IOException {
    String source = Files.readString(CONFIG);
    String normalized = source.replaceAll("\\s+", "");

    int adminIndex = normalized.indexOf(".requestMatchers(\"/api/admin/**\"");
    int notificationIndex =
        normalized.indexOf(
            ".requestMatchers(\"/api/user/notification/**\").hasAuthority(\"admin:all\")");
    int profileIndex =
        normalized.indexOf(
            ".requestMatchers(\"/api/user/profile/**\",\"/api/user/address/**\",\"/api/merchant/**\").authenticated()");

    assertThat(adminIndex).isGreaterThan(-1);
    assertThat(notificationIndex).isGreaterThan(-1);
    assertThat(profileIndex).isGreaterThan(-1);
    assertThat(notificationIndex).isLessThan(profileIndex);
    assertThat(source).doesNotContain(".requestMatchers(\"/api/user/**\").hasRole(\"USER\")");
  }
}
