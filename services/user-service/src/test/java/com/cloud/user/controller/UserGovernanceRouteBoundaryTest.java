package com.cloud.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.user.controller.user.UserManageController;
import com.cloud.user.controller.user.UserNotificationController;
import com.cloud.user.controller.user.UserQueryController;
import com.cloud.user.controller.user.UserStatisticsController;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

class UserGovernanceRouteBoundaryTest {

  private static final Map<Class<?>, String> INTERNAL_GOVERNANCE_CONTROLLERS =
      Map.of(
          ThreadPoolMonitorController.class, "/internal/user-governance/thread-pools",
          UserManageController.class, "/internal/user-governance/users",
          UserQueryController.class, "/internal/user-governance/users",
          UserStatisticsController.class, "/internal/user-governance/statistics",
          UserNotificationController.class, "/internal/user-governance/notifications");

  @Test
  void userGovernanceControllersUseInternalNamespace() {
    INTERNAL_GOVERNANCE_CONTROLLERS.forEach(
        (controller, expectedPath) -> {
          RequestMapping mapping = controller.getAnnotation(RequestMapping.class);

          assertThat(mapping).as(controller.getSimpleName()).isNotNull();
          assertThat(mapping.value()).containsExactly(expectedPath);
          assertThat(expectedPath).startsWith("/internal/user-governance/");
        });
  }

  @Test
  void userGovernanceControllersDoNotExposePublicAdminAggregatePaths() {
    INTERNAL_GOVERNANCE_CONTROLLERS.keySet().stream()
        .map(controller -> controller.getAnnotation(RequestMapping.class))
        .flatMap(mapping -> java.util.Arrays.stream(mapping.value()))
        .forEach(path -> assertThat(path).doesNotStartWith("/api/admin/"));
  }
}
