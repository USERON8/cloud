package com.cloud.governance.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityEntryService {

  @Value("${app.governance.observability.grafana.base-url:http://127.0.0.1:13000}")
  private String grafanaBaseUrl;

  @Value("${app.governance.observability.grafana.default-dashboard-uid:cloud-middleware-services}")
  private String defaultDashboardUid;

  @Value(
      "${app.governance.observability.grafana.default-dashboard-title:Cloud Middleware & Services}")
  private String defaultDashboardTitle;

  @Value("${app.governance.observability.prometheus.base-url:http://127.0.0.1:19099}")
  private String prometheusBaseUrl;

  @Value("${app.governance.observability.skywalking.base-url:http://127.0.0.1:13001}")
  private String skywalkingBaseUrl;

  public Map<String, Object> getGrafanaEntry() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("baseUrl", grafanaBaseUrl);
    result.put(
        "defaultDashboard",
        Map.of(
            "uid", defaultDashboardUid,
            "title", defaultDashboardTitle,
            "url", grafanaBaseUrl + "/d/" + defaultDashboardUid));
    result.put("relatedTools", List.of(prometheusBaseUrl, skywalkingBaseUrl));
    result.put(
        "notes",
        List.of(
            "Grafana remains the observability system of record",
            "governance-service only exposes governed entry metadata",
            "prefer dashboard drill-down for outbox and MQ backlog investigation"));
    return result;
  }
}
