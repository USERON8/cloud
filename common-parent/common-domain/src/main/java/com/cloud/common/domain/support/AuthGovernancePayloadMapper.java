package com.cloud.common.domain.support;

import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuthGovernancePayloadMapper {

  private AuthGovernancePayloadMapper() {}

  public static Map<String, Object> toTokenStatsPayload(AuthTokenStorageStatsVO stats) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("authorizationCount", stats.getAuthorizationCount());
    payload.put("accessIndexCount", stats.getAccessIndexCount());
    payload.put("refreshIndexCount", stats.getRefreshIndexCount());
    payload.put("codeIndexCount", stats.getCodeIndexCount());
    payload.put("principalIndexCount", stats.getPrincipalIndexCount());
    payload.put("redisInfo", stats.getRedisInfo());
    payload.put("storageType", stats.getStorageType());
    return payload;
  }

  public static Map<String, Object> toAuthorizationDetailPayload(AuthAuthorizationDetailVO detail) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", detail.getId());
    payload.put("clientId", detail.getClientId());
    payload.put("principalName", detail.getPrincipalName());
    payload.put("grantType", detail.getGrantType());
    payload.put("scopes", detail.getScopes());
    Map<String, Object> tokens = new LinkedHashMap<>();
    if (detail.getAccessToken() != null) {
      tokens.put(
          "accessToken",
          Map.of(
              "issuedAt", detail.getAccessToken().getIssuedAt(),
              "expiresAt", detail.getAccessToken().getExpiresAt(),
              "scopes", detail.getAccessToken().getScopes()));
    }
    if (detail.getRefreshToken() != null) {
      tokens.put(
          "refreshToken",
          Map.of(
              "issuedAt", detail.getRefreshToken().getIssuedAt(),
              "expiresAt", detail.getRefreshToken().getExpiresAt()));
    }
    payload.put("tokens", tokens);
    return payload;
  }

  public static Map<String, Object> toBlacklistCheckPayload(TokenBlacklistCheckVO result) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("tokenValue", result.getTokenPreview());
    payload.put("isBlacklisted", result.getBlacklisted());
    payload.put("checkTime", result.getCheckedAt());
    return payload;
  }
}
