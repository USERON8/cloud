package com.cloud.api.auth;

import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import java.util.Map;

public interface AuthGovernanceDubboApi {

  AuthTokenStorageStatsVO getTokenStats();

  AuthAuthorizationDetailVO getAuthorizationDetails(String authorizationId);

  Boolean revokeAuthorization(String authorizationId);

  Map<String, Object> cleanupAuthorizations();

  Map<String, Object> getAuthorizationStorageStructure();

  TokenBlacklistStatsVO getBlacklistStats();

  Boolean addToBlacklist(String tokenValue, String reason);

  TokenBlacklistCheckVO checkBlacklist(String tokenValue);

  Integer cleanupBlacklist();
}
