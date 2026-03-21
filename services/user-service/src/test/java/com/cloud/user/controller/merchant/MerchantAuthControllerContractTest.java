package com.cloud.user.controller.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MerchantAuthControllerContractTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/com/cloud/user/controller/merchant/MerchantAuthController.java");

  @Test
  void merchantAuthOwnerEndpointsShouldAllowAdminsOrOwnedMerchants() throws IOException {
    String source = Files.readString(CONTROLLER);
    String normalized = source.replaceAll("\\s+", "");
    String expectedExpression =
        "@PreAuthorize(\"hasAuthority('admin:all')\"+\"or(hasAuthority('merchant:manage')\"+\"and@permissionManager.isMerchantOwner(#merchantId,authentication))\")";

    assertThat(normalized).contains(expectedExpression);
    assertThat(normalized)
        .contains("applyForAuth(")
        .contains("uploadBusinessLicense(")
        .contains("getAuthInfo(")
        .contains("revokeAuth(");
  }
}
