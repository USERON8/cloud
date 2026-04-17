package com.cloud.governance.service;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.governance.config.ObservabilityProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityEntryService {

  private static final Pattern DASHBOARD_UID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,120}$");
  private final ObservabilityProperties observabilityProperties;

  public ObservabilityEntryService(ObservabilityProperties observabilityProperties) {
    this.observabilityProperties = observabilityProperties;
  }

  public Map<String, Object> getGrafanaEntry() {
    String grafanaBaseUrl = observabilityProperties.getGrafana().getBaseUrl();
    String defaultDashboardUid = observabilityProperties.getGrafana().getDefaultDashboardUid();
    String defaultDashboardTitle = observabilityProperties.getGrafana().getDefaultDashboardTitle();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("baseUrl", grafanaBaseUrl);
    result.put(
        "defaultDashboard",
        Map.of(
            "uid", defaultDashboardUid,
            "title", defaultDashboardTitle,
            "url", grafanaBaseUrl + "/d/" + defaultDashboardUid));
    result.put("allowedDashboards", getAllowedDashboards());
    result.put(
        "relatedTools",
        List.of(
            observabilityProperties.getPrometheus().getBaseUrl(),
            observabilityProperties.getSkywalking().getBaseUrl()));
    result.put(
        "notes",
        List.of(
            "Grafana remains the observability system of record",
            "governance-service exposes both governed entry metadata and whitelist-based jump URLs",
            "prefer dashboard drill-down for outbox and MQ backlog investigation"));
    return result;
  }

  public String resolveGrafanaUrl(String dashboardUid) {
    String targetDashboardUid = dashboardUid;
    if (targetDashboardUid == null || targetDashboardUid.isBlank()) {
      targetDashboardUid = observabilityProperties.getGrafana().getDefaultDashboardUid();
    }
    validateDashboardUid(targetDashboardUid);
    if (!observabilityProperties
        .getGrafana()
        .getAllowedDashboards()
        .containsKey(targetDashboardUid)) {
      throw new BizException(ResultCode.BAD_REQUEST, "dashboard uid is not allowed");
    }
    return observabilityProperties.getGrafana().getBaseUrl() + "/d/" + targetDashboardUid;
  }

  private void validateDashboardUid(String dashboardUid) {
    if (dashboardUid == null || dashboardUid.isBlank()) {
      throw new BizException(ResultCode.BAD_REQUEST, "dashboard uid is required");
    }
    if (!DASHBOARD_UID_PATTERN.matcher(dashboardUid).matches()) {
      throw new BizException(ResultCode.BAD_REQUEST, "dashboard uid is invalid");
    }
  }

  private List<Map<String, String>> getAllowedDashboards() {
    String grafanaBaseUrl = observabilityProperties.getGrafana().getBaseUrl();
    return observabilityProperties.getGrafana().getAllowedDashboards().entrySet().stream()
        .map(
            entry ->
                Map.of(
                    "uid", entry.getKey(),
                    "title", entry.getValue(),
                    "url", grafanaBaseUrl + "/d/" + entry.getKey()))
        .toList();
  }
}
