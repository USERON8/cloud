package com.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import com.cloud.auth.util.RedisKeyHelper;
import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.AuthTokenSummaryVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.exception.ResourceNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthGovernanceService {

  private final OAuth2AuthorizationService authorizationService;
  private final TokenBlacklistService tokenBlacklistService;
  private final OAuth2TokenManagementService tokenManagementService;

  @Qualifier("oauth2MainRedisTemplate")
  private final RedisTemplate<String, Object> redisTemplate;

  public AuthTokenStorageStatsVO getTokenStats() {
    return AuthTokenStorageStatsVO.builder()
        .authorizationCount(RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:token:*"))
        .accessIndexCount(RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:access:*"))
        .refreshIndexCount(RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:refresh:*"))
        .codeIndexCount(RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:code:*"))
        .principalIndexCount(RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:principal:*"))
        .redisInfo("Key/value storage mode")
        .storageType("Redis Key")
        .build();
  }

  public AuthAuthorizationDetailVO getAuthorizationDetails(String authorizationId) {
    String normalizedAuthorizationId = StrUtil.trim(authorizationId);
    OAuth2Authorization authorization = authorizationService.findById(normalizedAuthorizationId);
    if (authorization == null) {
      throw new ResourceNotFoundException("Authorization", normalizedAuthorizationId);
    }

    return AuthAuthorizationDetailVO.builder()
        .id(authorization.getId())
        .clientId(authorization.getRegisteredClientId())
        .principalName(authorization.getPrincipalName())
        .grantType(authorization.getAuthorizationGrantType().getValue())
        .scopes(authorization.getAuthorizedScopes())
        .accessToken(toSummary(authorization.getAccessToken()))
        .refreshToken(toSummary(authorization.getRefreshToken()))
        .build();
  }

  public Boolean revokeAuthorization(String authorizationId) {
    return tokenManagementService.revokeAuthorizationById(authorizationId, "governance_revocation");
  }

  public TokenBlacklistStatsVO getBlacklistStats() {
    TokenBlacklistService.BlacklistStats stats = tokenBlacklistService.getBlacklistStats();
    return TokenBlacklistStatsVO.builder()
        .totalBlacklisted(stats.totalBlacklisted)
        .activeBlacklisted(stats.activeBlacklisted)
        .lastUpdated(stats.lastUpdated)
        .build();
  }

  public Boolean addToBlacklist(String tokenValue, String reason) {
    String normalizedTokenValue = StrUtil.trim(tokenValue);
    OAuth2Authorization authorization =
        authorizationService.findByToken(normalizedTokenValue, null);
    String subject = authorization != null ? authorization.getPrincipalName() : "unknown";
    long ttlSeconds =
        tokenManagementService.resolveTokenTtlSeconds(authorization, normalizedTokenValue, 3600);
    tokenBlacklistService.addToBlacklist(normalizedTokenValue, subject, ttlSeconds, reason);
    return Boolean.TRUE;
  }

  public TokenBlacklistCheckVO checkBlacklist(String tokenValue) {
    String normalizedToken = StrUtil.nullToDefault(tokenValue, "").trim();
    String preview = normalizedToken.isEmpty() ? "" : maskToken(normalizedToken);
    return TokenBlacklistCheckVO.builder()
        .tokenPreview(preview)
        .blacklisted(tokenBlacklistService.isBlacklisted(tokenValue))
        .checkedAt(Instant.now())
        .build();
  }

  public Integer cleanupBlacklist() {
    return tokenBlacklistService.cleanupExpiredEntries();
  }

  private AuthTokenSummaryVO toSummary(
      OAuth2Authorization.Token<? extends org.springframework.security.oauth2.core.OAuth2Token>
          token) {
    if (token == null || token.getToken() == null) {
      return null;
    }
    return AuthTokenSummaryVO.builder()
        .issuedAt(token.getToken().getIssuedAt())
        .expiresAt(token.getToken().getExpiresAt())
        .scopes(
            token.getToken() instanceof OAuth2AccessToken accessToken
                ? accessToken.getScopes()
                : null)
        .build();
  }

  private String maskToken(String tokenValue) {
    int previewLength = Math.min(tokenValue.length(), 20);
    return tokenValue.substring(0, previewLength) + "...";
  }
}
