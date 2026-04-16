package com.cloud.governance.service;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.result.Result;
import com.cloud.common.security.InternalRequestHeaders;
import com.cloud.common.security.InternalRequestSigner;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class InternalOperationsClientSupport {

  private static final String INTERNAL_SUBJECT = "governance-service";
  private static final String INTERNAL_SCOPE = "internal";

  private final DiscoveryClient discoveryClient;
  private final RestClient restClient = RestClient.builder().build();

  @Value("${app.security.internal-hmac.secret:${app.security.signature.secret:}}")
  private String internalHmacSecret;

  public URI resolveUri(String serviceId, String path, Map<String, String> queryParams) {
    ServiceInstance instance = chooseInstance(serviceId);
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(instance.getUri()).path(path);
    queryParams.forEach(builder::queryParam);
    return builder.build(true).toUri();
  }

  public void applyInternalHeaders(HttpHeaders headers, String method, String path) {
    if (!StringUtils.hasText(internalHmacSecret)) {
      throw new IllegalStateException("internal HMAC secret is blank");
    }
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String signature =
        InternalRequestSigner.sign(
            method,
            path,
            timestamp,
            INTERNAL_SUBJECT,
            null,
            INTERNAL_SUBJECT,
            INTERNAL_SUBJECT,
            null,
            null,
            INTERNAL_SCOPE,
            internalHmacSecret);
    headers.set(InternalRequestHeaders.INTERNAL_REQUEST, "true");
    headers.set(InternalRequestHeaders.INTERNAL_SUBJECT, INTERNAL_SUBJECT);
    headers.set(InternalRequestHeaders.INTERNAL_USERNAME, INTERNAL_SUBJECT);
    headers.set(InternalRequestHeaders.INTERNAL_CLIENT_ID, INTERNAL_SUBJECT);
    headers.set(InternalRequestHeaders.INTERNAL_SCOPES, INTERNAL_SCOPE);
    headers.set(InternalRequestHeaders.INTERNAL_TIMESTAMP, timestamp);
    headers.set(InternalRequestHeaders.INTERNAL_SIGNATURE, signature);
  }

  public <T> Result<T> assertSuccess(Result<T> result, String serviceId, String path) {
    if (result == null) {
      throw RemoteException.providerError(
          ResultCode.REMOTE_SERVICE_ERROR,
          serviceId + path,
          new IllegalStateException("remote response is null"));
    }
    if (!result.isSuccess()) {
      throw RemoteException.providerError(
          ResultCode.REMOTE_SERVICE_ERROR,
          serviceId + path,
          new IllegalStateException(result.getMessage()));
    }
    return result;
  }

  public RemoteException translateRemoteError(String serviceId, String path, Exception ex) {
    if (ex instanceof ResourceAccessException resourceAccessException) {
      return RemoteException.unavailable(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, serviceId, resourceAccessException);
    }
    if (ex instanceof RestClientException restClientException) {
      return RemoteException.providerError(
          ResultCode.REMOTE_SERVICE_ERROR, serviceId + path, restClientException);
    }
    return RemoteException.providerError(ResultCode.REMOTE_SERVICE_ERROR, serviceId + path, ex);
  }

  public RestClient restClient() {
    return restClient;
  }

  private ServiceInstance chooseInstance(String serviceId) {
    List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
    if (instances == null || instances.isEmpty()) {
      throw RemoteException.unavailable(ResultCode.REMOTE_SERVICE_UNAVAILABLE, serviceId, null);
    }
    return instances.get(0);
  }
}
