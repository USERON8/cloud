package com.cloud.governance.service;

import com.cloud.api.auth.AuthGovernanceDubboApi;
import com.cloud.common.domain.support.AuthGovernancePayloadMapper;
import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.common.result.Result;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthGovernanceManagementService {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AuthGovernanceDubboApi authGovernanceDubboApi;

  private final RemoteCallSupport remoteCallSupport;

  public Result<Map<String, Object>> getTokenStats() {
    AuthTokenStorageStatsVO stats =
        remoteCallSupport.query(
            "auth-service.governance.getTokenStats", authGovernanceDubboApi::getTokenStats);
    return Result.success(AuthGovernancePayloadMapper.toTokenStatsPayload(stats));
  }

  public Result<Map<String, Object>> getAuthorizationStorageStructure() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getAuthorizationStorageStructure",
            authGovernanceDubboApi::getAuthorizationStorageStructure));
  }

  public Result<Map<String, Object>> getAuthorizationDetails(String id) {
    AuthAuthorizationDetailVO detail =
        remoteCallSupport.query(
            "auth-service.governance.getAuthorizationDetails",
            () -> authGovernanceDubboApi.getAuthorizationDetails(id));
    return Result.success(AuthGovernancePayloadMapper.toAuthorizationDetailPayload(detail));
  }

  public Result<Void> revokeAuthorization(String id) {
    remoteCallSupport.command(
        "auth-service.governance.revokeAuthorization",
        () -> authGovernanceDubboApi.revokeAuthorization(id));
    return Result.success();
  }

  public Result<Map<String, Object>> cleanupAuthorizations() {
    return Result.success(
        remoteCallSupport.command(
            "auth-service.governance.cleanupAuthorizations",
            authGovernanceDubboApi::cleanupAuthorizations));
  }

  public Result<TokenBlacklistStatsVO> getBlacklistStats() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getBlacklistStats",
            authGovernanceDubboApi::getBlacklistStats));
  }

  public Result<Void> addToBlacklist(String tokenValue, String reason) {
    remoteCallSupport.command(
        "auth-service.governance.addToBlacklist",
        () -> authGovernanceDubboApi.addToBlacklist(tokenValue, reason));
    return Result.success();
  }

  public Result<Map<String, Object>> checkBlacklist(String tokenValue) {
    TokenBlacklistCheckVO result =
        remoteCallSupport.query(
            "auth-service.governance.checkBlacklist",
            () -> authGovernanceDubboApi.checkBlacklist(tokenValue));
    return Result.success(AuthGovernancePayloadMapper.toBlacklistCheckPayload(result));
  }

  public Result<Map<String, Object>> cleanupBlacklist() {
    Integer cleanedCount =
        remoteCallSupport.command(
            "auth-service.governance.cleanupBlacklist", authGovernanceDubboApi::cleanupBlacklist);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("cleanedCount", cleanedCount);
    payload.put("message", "Blacklist cleanup completed");
    payload.put("cleanupTime", Instant.now());
    return Result.success(payload);
  }
}
