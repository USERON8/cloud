package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
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
}
