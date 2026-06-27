package com.cloud.governance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloud.common.exception.BizException;
import com.cloud.governance.config.ObservabilityProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ObservabilityEntryServiceTest {

  @Test
  void resolveGrafanaUrlUsesDefaultDashboardWhenUidIsBlank() {
    ObservabilityEntryService service = new ObservabilityEntryService(properties());

    assertThat(service.resolveGrafanaUrl(" ")).isEqualTo("http://grafana.local/d/cloud-overview");
  }

  @Test
  void resolveGrafanaUrlRejectsUnknownOrMalformedDashboardUid() {
    ObservabilityEntryService service = new ObservabilityEntryService(properties());

    assertThatThrownBy(() -> service.resolveGrafanaUrl("missing-dashboard"))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("dashboard uid is not allowed");

    assertThatThrownBy(() -> service.resolveGrafanaUrl("../admin"))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("dashboard uid is invalid");
  }

  @Test
  void getGrafanaEntryReturnsGovernedDashboardMetadata() {
    ObservabilityEntryService service = new ObservabilityEntryService(properties());

    Map<String, Object> entry = service.getGrafanaEntry();

    assertThat(entry).containsEntry("baseUrl", "http://grafana.local");
    assertThat(entry.get("defaultDashboard").toString()).contains("cloud-overview");
    assertThat(entry.get("allowedDashboards").toString()).contains("Order Flow");
    assertThat(entry.get("relatedTools").toString())
        .contains("http://prometheus.local", "http://skywalking.local");
  }

  private ObservabilityProperties properties() {
    ObservabilityProperties properties = new ObservabilityProperties();
    properties.getGrafana().setBaseUrl("http://grafana.local");
    properties.getGrafana().setDefaultDashboardUid("cloud-overview");
    properties.getGrafana().setDefaultDashboardTitle("Cloud Overview");

    Map<String, String> allowedDashboards = new LinkedHashMap<>();
    allowedDashboards.put("cloud-overview", "Cloud Overview");
    allowedDashboards.put("order-flow", "Order Flow");
    properties.getGrafana().setAllowedDashboards(allowedDashboards);

    properties.getPrometheus().setBaseUrl("http://prometheus.local");
    properties.getSkywalking().setBaseUrl("http://skywalking.local");
    return properties;
  }
}
