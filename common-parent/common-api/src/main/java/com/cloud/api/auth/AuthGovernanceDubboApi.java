package com.cloud.api.auth;

import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import java.util.Map;

/**
 * 认证治理内部调用契约。
 *
 * <p>治理服务通过该接口聚合 OAuth2 授权、令牌黑名单和存储结构信息，避免直接访问认证服务内部实现。
 */
public interface AuthGovernanceDubboApi {

  /** 获取令牌存储统计。 */
  AuthTokenStorageStatsVO getTokenStats();

  /** 查询指定授权记录详情。 */
  AuthAuthorizationDetailVO getAuthorizationDetails(String authorizationId);

  /** 撤销指定 OAuth2 授权。 */
  Boolean revokeAuthorization(String authorizationId);

  /** 清理过期授权记录并返回清理结果。 */
  Map<String, Object> cleanupAuthorizations();

  /** 查看授权数据在存储层的结构概览。 */
  Map<String, Object> getAuthorizationStorageStructure();

  /** 获取令牌黑名单统计。 */
  TokenBlacklistStatsVO getBlacklistStats();

  /** 手动将令牌加入黑名单。 */
  Boolean addToBlacklist(String tokenValue, String reason);

  /** 检查令牌是否已进入黑名单。 */
  TokenBlacklistCheckVO checkBlacklist(String tokenValue);

  /** 清理过期黑名单记录。 */
  Integer cleanupBlacklist();
}
