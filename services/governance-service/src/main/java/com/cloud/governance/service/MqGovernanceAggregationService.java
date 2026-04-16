package com.cloud.governance.service;

import com.cloud.common.result.Result;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqGovernanceAggregationService {

  private static final ParameterizedTypeReference<Result<List<Map<String, Object>>>>
      LIST_OF_MAP_RESULT_TYPE = new ParameterizedTypeReference<>() {};

  private static final ParameterizedTypeReference<Result<Boolean>> BOOLEAN_RESULT_TYPE =
      new ParameterizedTypeReference<>() {};

  private final InternalOperationsClientSupport clientSupport;

  @Value(
      "${app.governance.mq.service-ids:auth-service,user-service,product-service,search-service,order-service,payment-service,stock-service}")
  private List<String> serviceIds;

  public List<Map<String, Object>> listConsumers() {
    List<Map<String, Object>> aggregated = new ArrayList<>();
    for (String serviceId : serviceIds) {
      List<Map<String, Object>> items =
          getForList(serviceId, "/internal/mq/governance/consumers", Map.of());
      for (Map<String, Object> item : items) {
        aggregated.add(withServiceId(serviceId, item));
      }
    }
    aggregated.sort(
        Comparator.comparing(
                (Map<String, Object> item) -> String.valueOf(item.getOrDefault("serviceId", "")))
            .thenComparing(item -> String.valueOf(item.getOrDefault("topic", "")))
            .thenComparing(item -> String.valueOf(item.getOrDefault("consumerGroup", ""))));
    return aggregated;
  }

  public List<Map<String, Object>> listPendingDeadLetters(int limit) {
    List<Map<String, Object>> aggregated = new ArrayList<>();
    for (String serviceId : serviceIds) {
      List<Map<String, Object>> items =
          getForList(
              serviceId,
              "/internal/mq/dead-letters/pending",
              Map.of("limit", String.valueOf(limit)));
      for (Map<String, Object> item : items) {
        aggregated.add(withServiceId(serviceId, item));
      }
    }
    aggregated.sort(
        Comparator.comparing(
                (Map<String, Object> item) -> String.valueOf(item.getOrDefault("createdAt", "")),
                Comparator.reverseOrder())
            .thenComparing(item -> String.valueOf(item.getOrDefault("serviceId", ""))));
    return aggregated;
  }

  public boolean markDeadLetterHandled(String serviceId, String topic, String msgId) {
    Result<Boolean> result =
        postForResult(
            serviceId,
            "/internal/mq/dead-letters/handle",
            Map.of("topic", topic, "msgId", msgId),
            BOOLEAN_RESULT_TYPE);
    Boolean handled = result.getData();
    return Boolean.TRUE.equals(handled);
  }

  private List<Map<String, Object>> getForList(
      String serviceId, String path, Map<String, String> queryParams) {
    Result<List<Map<String, Object>>> result = getForResult(serviceId, path, queryParams);
    List<Map<String, Object>> data = result.getData();
    return data == null ? List.of() : data;
  }

  private Result<List<Map<String, Object>>> getForResult(
      String serviceId, String path, Map<String, String> queryParams) {
    URI uri = clientSupport.resolveUri(serviceId, path, queryParams);
    try {
      Result<List<Map<String, Object>>> result =
          clientSupport
              .restClient()
              .get()
              .uri(uri)
              .headers(headers -> clientSupport.applyInternalHeaders(headers, "GET", path))
              .retrieve()
              .body(LIST_OF_MAP_RESULT_TYPE);
      return clientSupport.assertSuccess(result, serviceId, path);
    } catch (Exception ex) {
      throw clientSupport.translateRemoteError(serviceId, path, ex);
    }
  }

  private <T> Result<T> postForResult(
      String serviceId,
      String path,
      Map<String, String> queryParams,
      ParameterizedTypeReference<Result<T>> responseType) {
    URI uri = clientSupport.resolveUri(serviceId, path, queryParams);
    try {
      Result<T> result =
          clientSupport
              .restClient()
              .post()
              .uri(uri)
              .headers(headers -> clientSupport.applyInternalHeaders(headers, "POST", path))
              .retrieve()
              .body(responseType);
      return clientSupport.assertSuccess(result, serviceId, path);
    } catch (Exception ex) {
      throw clientSupport.translateRemoteError(serviceId, path, ex);
    }
  }

  private Map<String, Object> withServiceId(String serviceId, Map<String, Object> source) {
    Map<String, Object> target = new LinkedHashMap<>();
    target.put("serviceId", serviceId);
    if (source != null) {
      target.putAll(source);
    }
    return target;
  }
}
