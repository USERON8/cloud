package com.cloud.governance.service;

import com.cloud.common.domain.dto.governance.OutboxBatchRequeueRequestDTO;
import com.cloud.common.messaging.outbox.OutboxEvent;
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
public class OutboxGovernanceAggregationService {

  private static final ParameterizedTypeReference<Result<Map<String, Object>>> MAP_RESULT_TYPE =
      new ParameterizedTypeReference<>() {};

  private static final ParameterizedTypeReference<Result<List<OutboxEvent>>>
      OUTBOX_LIST_RESULT_TYPE = new ParameterizedTypeReference<>() {};

  private static final ParameterizedTypeReference<Result<Boolean>> BOOLEAN_RESULT_TYPE =
      new ParameterizedTypeReference<>() {};

  private static final ParameterizedTypeReference<Result<Integer>> INTEGER_RESULT_TYPE =
      new ParameterizedTypeReference<>() {};

  private final InternalOperationsClientSupport clientSupport;

  @Value(
      "${app.governance.outbox.service-ids:user-service,order-service,payment-service,stock-service,product-service,search-service,auth-service}")
  private List<String> serviceIds;

  public List<Map<String, Object>> getStats() {
    List<Map<String, Object>> aggregated = new ArrayList<>();
    for (String serviceId : serviceIds) {
      Result<Map<String, Object>> result =
          getForResult(serviceId, "/internal/outbox/governance/stats", Map.of(), MAP_RESULT_TYPE);
      aggregated.add(withServiceId(serviceId, result.getData()));
    }
    aggregated.sort(
        Comparator.comparing(
            (Map<String, Object> item) -> String.valueOf(item.getOrDefault("serviceId", ""))));
    return aggregated;
  }

  public List<Map<String, Object>> listPending(int limit) {
    return listEvents("/internal/outbox/governance/pending", limit);
  }

  public List<Map<String, Object>> listDead(int limit) {
    return listEvents("/internal/outbox/governance/dead", limit);
  }

  public boolean requeue(String serviceId, Long id) {
    Result<Boolean> result =
        postForResult(
            serviceId,
            "/internal/outbox/governance/requeue",
            Map.of("id", String.valueOf(id)),
            BOOLEAN_RESULT_TYPE);
    return Boolean.TRUE.equals(result.getData());
  }

  public int requeueBatch(String serviceId, List<Long> ids) {
    OutboxBatchRequeueRequestDTO requestDTO = new OutboxBatchRequeueRequestDTO();
    requestDTO.setIds(ids);
    Result<Integer> result =
        postBodyForResult(
            serviceId,
            "/internal/outbox/governance/requeue-batch",
            requestDTO,
            INTEGER_RESULT_TYPE);
    return result.getData() == null ? 0 : result.getData();
  }

  private List<Map<String, Object>> listEvents(String path, int limit) {
    List<Map<String, Object>> aggregated = new ArrayList<>();
    for (String serviceId : serviceIds) {
      Result<List<OutboxEvent>> result =
          getForResult(
              serviceId, path, Map.of("limit", String.valueOf(limit)), OUTBOX_LIST_RESULT_TYPE);
      List<OutboxEvent> events = result.getData();
      if (events == null) {
        continue;
      }
      for (OutboxEvent event : events) {
        aggregated.add(asMap(serviceId, event));
      }
    }
    aggregated.sort(
        Comparator.comparing(
                (Map<String, Object> item) -> String.valueOf(item.getOrDefault("createdAt", "")),
                Comparator.reverseOrder())
            .thenComparing(item -> String.valueOf(item.getOrDefault("serviceId", ""))));
    return aggregated;
  }

  private <T> Result<T> getForResult(
      String serviceId,
      String path,
      Map<String, String> queryParams,
      ParameterizedTypeReference<Result<T>> responseType) {
    URI uri = clientSupport.resolveUri(serviceId, path, queryParams);
    try {
      Result<T> result =
          clientSupport
              .restClient()
              .get()
              .uri(uri)
              .headers(headers -> clientSupport.applyInternalHeaders(headers, "GET", path))
              .retrieve()
              .body(responseType);
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

  private <T> Result<T> postBodyForResult(
      String serviceId,
      String path,
      Object body,
      ParameterizedTypeReference<Result<T>> responseType) {
    URI uri = clientSupport.resolveUri(serviceId, path, Map.of());
    try {
      Result<T> result =
          clientSupport
              .restClient()
              .post()
              .uri(uri)
              .headers(headers -> clientSupport.applyInternalHeaders(headers, "POST", path))
              .body(body)
              .retrieve()
              .body(responseType);
      return clientSupport.assertSuccess(result, serviceId, path);
    } catch (Exception ex) {
      throw clientSupport.translateRemoteError(serviceId, path, ex);
    }
  }

  private Map<String, Object> withServiceId(String serviceId, Map<String, Object> payload) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("serviceId", serviceId);
    if (payload != null) {
      result.putAll(payload);
    }
    return result;
  }

  private Map<String, Object> asMap(String serviceId, OutboxEvent event) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("serviceId", serviceId);
    result.put("id", event.getId());
    result.put("eventId", event.getEventId());
    result.put("aggregateType", event.getAggregateType());
    result.put("aggregateId", event.getAggregateId());
    result.put("eventType", event.getEventType());
    result.put("status", event.getStatus());
    result.put("retryCount", event.getRetryCount());
    result.put("nextRetryAt", event.getNextRetryAt());
    result.put("createdAt", event.getCreatedAt());
    result.put("updatedAt", event.getUpdatedAt());
    return result;
  }
}
