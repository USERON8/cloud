package com.cloud.auth.rpc;

import com.cloud.api.auth.AuthGovernanceDubboApi;
import com.cloud.auth.service.AuthGovernanceService;
import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = AuthGovernanceDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AuthGovernanceDubboService implements AuthGovernanceDubboApi {

  private final AuthGovernanceService authGovernanceService;

  @Override
  public AuthTokenStorageStatsVO getTokenStats() {
    return authGovernanceService.getTokenStats();
  }

  @Override
  public AuthAuthorizationDetailVO getAuthorizationDetails(String authorizationId) {
    return authGovernanceService.getAuthorizationDetails(authorizationId);
  }

  @Override
  public Boolean revokeAuthorization(String authorizationId) {
    return authGovernanceService.revokeAuthorization(authorizationId);
  }

  @Override
  public Map<String, Object> cleanupAuthorizations() {
    return authGovernanceService.cleanupAuthorizations();
  }

  @Override
  public Map<String, Object> getAuthorizationStorageStructure() {
    return authGovernanceService.getAuthorizationStorageStructure();
  }

  @Override
  public TokenBlacklistStatsVO getBlacklistStats() {
    return authGovernanceService.getBlacklistStats();
  }

  @Override
  public Boolean addToBlacklist(String tokenValue, String reason) {
    return authGovernanceService.addToBlacklist(tokenValue, reason);
  }

  @Override
  public TokenBlacklistCheckVO checkBlacklist(String tokenValue) {
    return authGovernanceService.checkBlacklist(tokenValue);
  }

  @Override
  public Integer cleanupBlacklist() {
    return authGovernanceService.cleanupBlacklist();
  }
}
