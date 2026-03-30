package com.cloud.auth.controller;

import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.service.TokenBlacklistService;
import com.cloud.auth.util.RedisKeyHelper;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/tokens")
@Tag(name = "OAuth2 Token Management", description = "OAuth2 token management and monitoring APIs")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid token management parameters"),
  @ApiResponse(responseCode = "401", description = "Authentication required"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Authorization or token resource not found"),
  @ApiResponse(responseCode = "500", description = "Internal token management error")
})
public class OAuth2TokenManageController {

  private final OAuth2AuthorizationService authorizationService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final TokenBlacklistService tokenBlacklistService;
  private final OAuth2TokenManagementService tokenManagementService;

  public OAuth2TokenManageController(
      OAuth2AuthorizationService authorizationService,
      @Qualifier("oauth2MainRedisTemplate") RedisTemplate<String, Object> redisTemplate,
      TokenBlacklistService tokenBlacklistService,
      OAuth2TokenManagementService tokenManagementService) {
    this.authorizationService = authorizationService;
    this.redisTemplate = redisTemplate;
    this.tokenBlacklistService = tokenBlacklistService;
    this.tokenManagementService = tokenManagementService;
  }

  @Operation(
      summary = "Get token storage statistics",
      description = "Get token storage metrics from Redis")
  @GetMapping("/stats")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getTokenStats() {
    Map<String, Object> stats = new HashMap<>();
    long tokenCount = RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:token:*");
    long accessIndexCount = RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:access:*");
    long refreshCount = RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:refresh:*");
    long codeCount = RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:code:*");
    long principalIndexCount =
        RedisKeyHelper.countKeysByPattern(redisTemplate, "oauth2:principal:*");

    stats.put("authorizationCount", tokenCount);
    stats.put("accessIndexCount", accessIndexCount);
    stats.put("refreshIndexCount", refreshCount);
    stats.put("codeIndexCount", codeCount);
    stats.put("principalIndexCount", principalIndexCount);
    stats.put("redisInfo", "Key/value storage mode");
    stats.put("storageType", "Redis Key");

    return Result.success(stats);
  }

  @Operation(
      summary = "Get authorization details",
      description = "Get authorization details by authorization ID")
  @GetMapping("/authorization/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getAuthorizationDetails(
      @Parameter(description = "Authorization ID")
          @PathVariable
          @NotBlank(message = "authorization id cannot be blank")
          String id) {
    OAuth2Authorization authorization = authorizationService.findById(id);
    if (authorization == null) {
      log.warn("Authorization not found: id={}", id);
      throw new ResourceNotFoundException("Authorization", id);
    }

    Map<String, Object> details = new HashMap<>();
    details.put("id", authorization.getId());
    details.put("clientId", authorization.getRegisteredClientId());
    details.put("principalName", authorization.getPrincipalName());
    details.put("grantType", authorization.getAuthorizationGrantType().getValue());
    details.put("scopes", authorization.getAuthorizedScopes());

    Map<String, Object> tokens = new HashMap<>();
    if (authorization.getAccessToken() != null) {
      tokens.put(
          "accessToken",
          Map.of(
              "issuedAt", authorization.getAccessToken().getToken().getIssuedAt(),
              "expiresAt", authorization.getAccessToken().getToken().getExpiresAt(),
              "scopes", authorization.getAccessToken().getToken().getScopes()));
    }
    if (authorization.getRefreshToken() != null) {
      tokens.put(
          "refreshToken",
          Map.of(
              "issuedAt", authorization.getRefreshToken().getToken().getIssuedAt(),
              "expiresAt", authorization.getRefreshToken().getToken().getExpiresAt()));
    }
    details.put("tokens", tokens);

    return Result.success(details);
  }

  @Operation(summary = "Revoke authorization", description = "Revoke OAuth2 authorization by ID")
  @DeleteMapping("/authorization/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> revokeAuthorization(
      @Parameter(description = "Authorization ID")
          @PathVariable
          @NotBlank(message = "authorization id cannot be blank")
          String id) {
    boolean revoked = tokenManagementService.revokeAuthorizationById(id, "admin_revocation");
    if (!revoked) {
      log.warn("Authorization not found: id={}", id);
      throw new ResourceNotFoundException("Authorization", id);
    }
    return Result.success();
  }

  @Operation(
      summary = "Cleanup expired tokens",
      description = "Trigger cleanup for expired token data")
  @PostMapping("/cleanup")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupExpiredTokens() {
    Map<String, Object> result = new HashMap<>();
    result.put("message", "Cleanup job executed");
    result.put("note", "Redis TTL will automatically remove expired data");
    result.put("time", Instant.now());
    return Result.success(result);
  }

  @Operation(
      summary = "Get Redis hash storage structure",
      description = "Show Redis hash structure for OAuth2 data")
  @GetMapping("/storage-structure")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getStorageStructure() {
    Map<String, Object> structure = new HashMap<>();

    Map<String, String> hashExample = new HashMap<>();
    hashExample.put("oauth2:token:{authorizationId}", "Serialized OAuth2Authorization");
    hashExample.put("oauth2:access:{accessToken}", "Access token index to authorization ID");
    hashExample.put("oauth2:refresh:{refreshTokenId}", "Refresh token index to authorization ID");
    hashExample.put("oauth2:code:{code}", "Authorization code index to authorization ID");
    hashExample.put("oauth2:principal:{username}", "Principal to authorization ID set");

    Map<String, String> tokenIndex = new HashMap<>();
    tokenIndex.put("oauth2:token:{authorizationId}", "Authorization object storage");
    tokenIndex.put("oauth2:access:{accessToken}", "Access token index");
    tokenIndex.put("oauth2:refresh:{refreshTokenId}", "Refresh token index");
    tokenIndex.put("oauth2:code:{code}", "Authorization code index");
    tokenIndex.put("oauth2:principal:{username}", "Principal authorization set");

    structure.put("keys", hashExample);
    structure.put("tokenIndexes", tokenIndex);
    structure.put(
        "advantages",
        java.util.List.of(
            "Simple key/value storage",
            "Direct index for access tokens",
            "Token TTL aligned with Redis TTL",
            "Easy manual inspection",
            "Direct index for refresh/code tokens",
            "Direct principal to authorization lookup"));
    return Result.success(structure);
  }

  @Operation(
      summary = "Get blacklist statistics",
      description = "Get current token blacklist statistics")
  @GetMapping("/blacklist/stats")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<TokenBlacklistService.BlacklistStats> getBlacklistStats() {
    return Result.success(tokenBlacklistService.getBlacklistStats());
  }

  @Operation(summary = "Add token to blacklist", description = "Manually add a token to blacklist")
  @PostMapping("/blacklist/add")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> addToBlacklist(
      @Parameter(description = "Token value")
          @RequestParam
          @NotBlank(message = "tokenValue cannot be blank")
          String tokenValue,
      @Parameter(description = "Revocation reason")
          @RequestParam(defaultValue = "admin_manual")
          @NotBlank(message = "reason cannot be blank")
          @Size(max = 64, message = "reason must be less than or equal to 64 characters")
          String reason) {
    OAuth2Authorization authorization = authorizationService.findByToken(tokenValue, null);
    String subject = authorization != null ? authorization.getPrincipalName() : "unknown";
    long ttlSeconds =
        tokenManagementService.resolveTokenTtlSeconds(authorization, tokenValue, 3600);
    tokenBlacklistService.addToBlacklist(tokenValue, subject, ttlSeconds, reason);
    return Result.success();
  }

  @Operation(
      summary = "Check blacklist status",
      description = "Check whether a token is blacklisted")
  @GetMapping("/blacklist/check")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> checkBlacklist(
      @Parameter(description = "Token value")
          @RequestParam
          @NotBlank(message = "tokenValue cannot be blank")
          String tokenValue) {
    boolean isBlacklisted = tokenBlacklistService.isBlacklisted(tokenValue);
    Map<String, Object> result = new HashMap<>();
    result.put("tokenValue", tokenValue.substring(0, Math.min(tokenValue.length(), 20)) + "...");
    result.put("isBlacklisted", isBlacklisted);
    result.put("checkTime", Instant.now());
    return Result.success(result);
  }

  @Operation(
      summary = "Cleanup blacklist entries",
      description = "Remove expired blacklist entries")
  @PostMapping("/blacklist/cleanup")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupBlacklist() {
    int cleanedCount = tokenBlacklistService.cleanupExpiredEntries();
    Map<String, Object> result = new HashMap<>();
    result.put("cleanedCount", cleanedCount);
    result.put("message", "Blacklist cleanup completed");
    result.put("cleanupTime", Instant.now());
    return Result.success(result);
  }
}
