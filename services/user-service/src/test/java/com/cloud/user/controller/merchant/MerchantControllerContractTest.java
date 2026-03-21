package com.cloud.user.controller.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MerchantControllerContractTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/com/cloud/user/controller/merchant/MerchantController.java");

  @Test
  void merchantDetailEndpointsShouldAllowAdminsOrOwnedMerchants() throws IOException {
    String source = Files.readString(CONTROLLER);
    String normalized = source.replaceAll("\\s+", "");
    String expectedExpression =
        "@PreAuthorize(\"hasAuthority('admin:all')\"+\"or(hasAuthority('merchant:manage')\"+\"and@permissionManager.isMerchantOwner(#id,authentication))\")";

    assertThat(normalized).contains(expectedExpression);
    assertThat(normalized)
        .contains("getMerchantById(")
        .contains("updateMerchant(")
        .contains("getMerchantStatistics(");
  }
}
