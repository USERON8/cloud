package com.cloud.governance.service;

import com.cloud.common.result.Result;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GovernanceAggregationHttpSupport {

  private final InternalOperationsClientSupport clientSupport;

  public <T> Result<T> getForResult(
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

  public <T> Result<T> postForResult(
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

  public <T> Result<T> postBodyForResult(
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

  public Map<String, Object> withServiceId(String serviceId, Map<String, Object> payload) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("serviceId", serviceId);
    if (payload != null) {
      result.putAll(payload);
    }
    return result;
  }
}
