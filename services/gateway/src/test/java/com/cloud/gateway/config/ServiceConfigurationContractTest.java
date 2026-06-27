package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ServiceConfigurationContractTest {

  private static final List<String> COMMITTED_SECRET_VALUES =
      List.of(
          "cloud-gateway-signature-dev",
          "cloud-shop-secret",
          "cloud-mini-secret",
          "cloud-client-service-secret-dev",
          "TEST_ENV_PERMANENT_TOKEN");

  @Test
  void productionLikeConfigsDoNotContainCommittedSecretFallbacks() throws Exception {
    for (Path config : productionLikeConfigFiles()) {
      String content = Files.readString(config, StandardCharsets.UTF_8);

      assertThat(content)
          .as("production-like config must not contain committed secret fallback: %s", config)
          .doesNotContain(COMMITTED_SECRET_VALUES.toArray(String[]::new));
    }
  }

  @Test
  void serviceConfigsImportCommonAndServiceSpecificNacosConfig() throws Exception {
    Path services = repoRoot().resolve("services");
    try (Stream<Path> stream = Files.list(services)) {
      for (Path serviceDir : stream.filter(Files::isDirectory).toList()) {
        Path application = serviceDir.resolve("src/main/resources/application.yml");
        if (!Files.exists(application)) {
          continue;
        }
        String serviceName = serviceDir.getFileName().toString();
        String content = Files.readString(application, StandardCharsets.UTF_8);

        assertThat(content)
            .as("%s must import common Nacos config", application)
            .contains("common.yaml");
        assertThat(content)
            .as("%s must import service-specific Nacos config", application)
            .contains("optional:nacos:" + serviceName + ".yaml");
      }
    }
  }

  @Test
  void commonNacosTemplateDoesNotContainServicePrivateOAuthSecrets() throws Exception {
    Path commonNacos = repoRoot().resolve("docs/nacos/common.yaml");
    String content = Files.readString(commonNacos, StandardCharsets.UTF_8);

    assertThat(content).doesNotContain("APP_OAUTH2_WEB_CLIENT_SECRET");
    assertThat(content).doesNotContain("APP_OAUTH2_MOBILE_CLIENT_SECRET");
    assertThat(content).doesNotContain("APP_OAUTH2_INTERNAL_CLIENT_SECRET");
    assertThat(content).doesNotContain("CLIENT_SERVICE_SECRET");
  }

  private List<Path> productionLikeConfigFiles() throws IOException {
    Path root = repoRoot();
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
          .filter(
              path ->
                  path.toString().contains("\\src\\main\\resources\\")
                      || path.toString().contains("\\docs\\nacos\\"))
          .filter(path -> !path.getFileName().toString().contains("-dev"))
          .filter(path -> !path.getFileName().toString().contains("-test"))
          .filter(path -> !path.toString().contains("\\target\\"))
          .toList();
    }
  }

  private Path repoRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("services/pom.xml"))) {
      current = current.getParent();
    }
    assertThat(current).as("repository root").isNotNull();
    return current;
  }
}
