package com.cloud.governance.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.governance.observability")
public class ObservabilityProperties {

  private final Grafana grafana = new Grafana();
  private final Endpoint prometheus = new Endpoint();
  private final Endpoint skywalking = new Endpoint();

  @Getter
  @Setter
  public static class Grafana {

    private String baseUrl = "http://127.0.0.1:13000";
    private String defaultDashboardUid = "cloud-middleware-services";
    private String defaultDashboardTitle = "Cloud Middleware & Services";
    private Map<String, String> allowedDashboards = new LinkedHashMap<>();
  }

  @Getter
  @Setter
  public static class Endpoint {

    private String baseUrl;
  }
}
