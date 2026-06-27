package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;

class GatewayRouteDefinitionTest {

  @Test
  void orderServiceRouteIncludesCartRootAndNestedPath() throws Exception {
    Map<String, Object> root = new Yaml().load(readResource("application-route.yml"));
    Map<String, Object> spring = asMap(root.get("spring"));
    Map<String, Object> cloud = asMap(spring.get("cloud"));
    Map<String, Object> gateway = asMap(cloud.get("gateway"));
    Map<String, Object> server = asMap(gateway.get("server"));
    Map<String, Object> webflux = asMap(server.get("webflux"));
    List<Map<String, Object>> routes = asRouteList(webflux.get("routes"));

    Map<String, Object> orderRoute =
        routes.stream()
            .filter(route -> "order-service-app".equals(route.get("id")))
            .findFirst()
            .orElseThrow();

    assertThat(asStringList(orderRoute.get("predicates")))
        .contains(
            "Path=/api/orders/**,/api/users/me/cart,/api/users/me/cart/**,/api/after-sales/**");
  }

  @Test
  void orderServiceCartRouteHasHigherPriorityThanUserRoute() throws Exception {
    Map<String, Object> root = new Yaml().load(readResource("application-route.yml"));
    Map<String, Object> spring = asMap(root.get("spring"));
    Map<String, Object> cloud = asMap(spring.get("cloud"));
    Map<String, Object> gateway = asMap(cloud.get("gateway"));
    Map<String, Object> server = asMap(gateway.get("server"));
    Map<String, Object> webflux = asMap(server.get("webflux"));
    List<Map<String, Object>> routes = asRouteList(webflux.get("routes"));

    Map<String, Object> orderRoute =
        routes.stream()
            .filter(route -> "order-service-app".equals(route.get("id")))
            .findFirst()
            .orElseThrow();
    Map<String, Object> userRoute =
        routes.stream()
            .filter(route -> "user-service-app".equals(route.get("id")))
            .findFirst()
            .orElseThrow();

    assertThat(asInteger(orderRoute.get("order"))).isLessThan(asInteger(userRoute.get("order")));
  }

  @Test
  void routeIdsAreUnique() throws Exception {
    List<Map<String, Object>> routes = routes();

    List<Object> routeIds = routes.stream().map(route -> route.get("id")).toList();

    assertThat(routeIds).doesNotHaveDuplicates();
  }

  @Test
  void governanceCompatibilityProxyRouteIsRemoved() throws Exception {
    List<Map<String, Object>> routes = routes();

    assertThat(routes.stream().map(route -> route.get("id")))
        .doesNotContain("governance-service-admin-proxy");
    assertThat(
            routes.stream()
                .flatMap(route -> asStringList(route.get("predicates")).stream()))
        .noneMatch(predicate -> predicate.contains("/api/admin/governance/**"));
  }

  @Test
  void internalHttpGovernanceRoutesAreRemoved() throws Exception {
    List<Map<String, Object>> routes = routes();

    assertThat(routes.stream().map(route -> route.get("id")))
        .doesNotContain("stock-service-admin", "governance-service-internal");
    assertThat(
            routes.stream()
                .flatMap(route -> asStringList(route.get("predicates")).stream()))
        .noneMatch(predicate -> predicate.contains("/internal/governance/**"))
        .noneMatch(predicate -> predicate.contains("/api/admin/stocks/internal/**"))
        .noneMatch(predicate -> predicate.contains("/api/admin/statistics/internal/**"))
        .noneMatch(predicate -> predicate.contains("/api/admin/thread-pool/internal/**"));
  }

  @Test
  void authGovernanceRoutesBelongToGovernanceService() throws Exception {
    Map<String, Object> governanceRoute = routeById("governance-service-admin");
    Map<String, Object> authRoute = routeById("auth-service-api");

    assertThat(asInteger(governanceRoute.get("order")))
        .isLessThan(asInteger(authRoute.get("order")));
    assertThat(asStringList(governanceRoute.get("predicates")))
        .contains(
            "Path=/auth/authorizations/**,/auth/cleanups/**,/auth/blacklist-entries/**,/api/admin/thread-pools/**,/api/admin/statistics/**,/api/admin/users/**,/api/admin/mq/**,/api/admin/outbox/**,/api/admin/observability/**,/api/admin/notifications/**");
  }

  @Test
  void paymentAppRouteUsesPaymentFallback() throws Exception {
    Map<String, Object> paymentRoute = routeById("payment-service-app");

    assertThat(flattenFilters(paymentRoute))
        .contains("name=CircuitBreaker")
        .contains("fallbackUri=forward:/gateway/fallback/payment");
  }

  @Test
  void paymentCallbackRouteDoesNotUsePaymentFallback() throws Exception {
    Map<String, Object> paymentCallbackRoute = routeById("payment-service-callback");

    assertThat(flattenFilters(paymentCallbackRoute))
        .doesNotContain("fallbackUri=forward:/gateway/fallback/payment");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object value) {
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> asRouteList(Object value) {
    return (List<Map<String, Object>>) value;
  }

  @SuppressWarnings("unchecked")
  private List<String> asStringList(Object value) {
    return (List<String>) value;
  }

  private Integer asInteger(Object value) {
    return (Integer) value;
  }

  private String readResource(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
  }

  private Map<String, Object> routeById(String routeId) throws Exception {
    return routes().stream()
        .filter(route -> routeId.equals(route.get("id")))
        .findFirst()
        .orElseThrow();
  }

  private List<Map<String, Object>> routes() throws Exception {
    Map<String, Object> root = new Yaml().load(readResource("application-route.yml"));
    Map<String, Object> spring = asMap(root.get("spring"));
    Map<String, Object> cloud = asMap(spring.get("cloud"));
    Map<String, Object> gateway = asMap(cloud.get("gateway"));
    Map<String, Object> server = asMap(gateway.get("server"));
    Map<String, Object> webflux = asMap(server.get("webflux"));
    return asRouteList(webflux.get("routes"));
  }

  private List<String> flattenFilters(Map<String, Object> route) {
    List<String> flattened = new ArrayList<>();
    for (Object filter : asStringOrMapList(route.get("filters"))) {
      if (filter instanceof String text) {
        flattened.add(text);
      } else if (filter instanceof Map<?, ?> map) {
        map.forEach((key, value) -> flattenFilterEntry(flattened, key, value));
      }
    }
    return flattened;
  }

  private void flattenFilterEntry(List<String> flattened, Object key, Object value) {
    if (value instanceof Map<?, ?> map) {
      map.forEach(
          (nestedKey, nestedValue) -> flattenFilterEntry(flattened, nestedKey, nestedValue));
      return;
    }
    flattened.add(key + "=" + value);
  }

  @SuppressWarnings("unchecked")
  private List<Object> asStringOrMapList(Object value) {
    return (List<Object>) value;
  }
}
