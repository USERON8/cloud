package com.cloud.governance.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class GovernanceAdminRouteBoundaryTest {

  @Test
  void governanceControllerDoesNotExposeAdminAccountCrudProxy() {
    assertThat(controllerPaths()).noneMatch(path -> path.startsWith("/api/admins"));
  }

  @Test
  void governanceControllerKeepsPublicAdminAggregateRoutes() {
    assertThat(controllerPaths())
        .contains(
            "/api/admin/users",
            "/api/admin/statistics/overview",
            "/api/admin/thread-pools",
            "/api/admin/notifications/system",
            "/api/admin/outbox/stats");
  }

  private Stream<String> controllerPaths() {
    return Arrays.stream(GovernanceAdminController.class.getDeclaredMethods())
        .flatMap(this::methodPaths);
  }

  private Stream<String> methodPaths(Method method) {
    Stream<String> get = values(method.getAnnotation(GetMapping.class));
    Stream<String> post = values(method.getAnnotation(PostMapping.class));
    Stream<String> put = values(method.getAnnotation(PutMapping.class));
    Stream<String> delete = values(method.getAnnotation(DeleteMapping.class));
    Stream<String> patch = values(method.getAnnotation(PatchMapping.class));
    return Stream.of(get, post, put, delete, patch).flatMap(paths -> paths);
  }

  private Stream<String> values(GetMapping mapping) {
    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
  }

  private Stream<String> values(PostMapping mapping) {
    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
  }

  private Stream<String> values(PutMapping mapping) {
    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
  }

  private Stream<String> values(DeleteMapping mapping) {
    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
  }

  private Stream<String> values(PatchMapping mapping) {
    return mapping == null ? Stream.empty() : Arrays.stream(mapping.value());
  }
}
