package com.cloud.auth.controller;

import com.cloud.auth.service.SimpleRedisHashOAuth2AuthorizationService;
import com.cloud.auth.service.TokenBlacklistService;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/auth/tokens")
@Tag(name = "OAuth2 Token Management", description = "OAuth2 token management and monitoring APIs")
public class OAuth2TokenManageController {

    private final OAuth2AuthorizationService authorizationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenBlacklistService tokenBlacklistService;

    public OAuth2TokenManageController(
            OAuth2AuthorizationService authorizationService,
            @Qualifier("oauth2MainRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            TokenBlacklistService tokenBlacklistService) {
        this.authorizationService = authorizationService;
        this.redisTemplate = redisTemplate;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Operation(summary = "Get token storage statistics", description = "Get token storage metrics from Redis")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getTokenStats() {
        Map<String, Object> stats = new HashMap<>();
        Set<String> authKeys = redisTemplate.keys("oauth2:auth:*");
        Set<String> tokenKeys = redisTemplate.keys("oauth2:token:*");

        stats.put("authorizationCount", authKeys != null ? authKeys.size() : 0);
        stats.put("tokenIndexCount", tokenKeys != null ? tokenKeys.size() : 0);
        stats.put("redisInfo", "Hash storage mode");
        stats.put("storageType", "Redis Hash");

        if (authorizationService instanceof SimpleRedisHashOAuth2AuthorizationService hashService) {
            stats.put("serviceStats", hashService.getStorageStats());
        }

        return Result.success(stats);
    }

    @Operation(summary = "Get authorization details", description = "Get authorization details by authorization ID")
    @GetMapping("/authorization/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAuthorizationDetails(
            @Parameter(description = "Authorization ID") @PathVariable String id) {
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
            tokens.put("accessToken", Map.of(
                    "issuedAt", authorization.getAccessToken().getToken().getIssuedAt(),
                    "expiresAt", authorization.getAccessToken().getToken().getExpiresAt(),
                    "scopes", authorization.getAccessToken().getToken().getScopes()
            ));
        }
        if (authorization.getRefreshToken() != null) {
            tokens.put("refreshToken", Map.of(
                    "issuedAt", authorization.getRefreshToken().getToken().getIssuedAt(),
                    "expiresAt", authorization.getRefreshToken().getToken().getExpiresAt()
            ));
        }
        details.put("tokens", tokens);

        return Result.success(details);
    }

    @Operation(summary = "Revoke authorization", description = "Revoke OAuth2 authorization by ID")
    @DeleteMapping("/authorization/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> revokeAuthorization(
            @Parameter(description = "Authorization ID") @PathVariable String id) {
        OAuth2Authorization authorization = authorizationService.findById(id);
        if (authorization == null) {
            log.warn("Authorization not found: id={}", id);
            throw new ResourceNotFoundException("Authorization", id);
        }

        authorizationService.remove(authorization);
        return Result.success();
    }

    @Operation(summary = "Cleanup expired tokens", description = "Trigger cleanup for expired token data")
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> cleanupExpiredTokens() {
        if (authorizationService instanceof SimpleRedisHashOAuth2AuthorizationService hashService) {
            hashService.cleanupExpiredTokens();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Cleanup job executed");
        result.put("note", "Redis TTL will automatically remove expired data");
        result.put("time", Instant.now());
        return Result.success(result);
    }

    @Operation(summary = "Get Redis hash storage structure", description = "Show Redis hash structure for OAuth2 data")
    @GetMapping("/storage-structure")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStorageStructure() {
        Map<String, Object> structure = new HashMap<>();

        Map<String, String> hashExample = new HashMap<>();
        hashExample.put("oauth2:auth:{authorizationId}", "Redis hash key for complete authorization data");

        Map<String, String> hashFields = new HashMap<>();
        hashFields.put("data", "Serialized OAuth2Authorization JSON payload");
        hashFields.put("clientId", "Client ID for query optimization");
        hashFields.put("principalName", "Principal name for query optimization");
        hashFields.put("createTime", "Creation timestamp");

        Map<String, String> tokenIndex = new HashMap<>();
        tokenIndex.put("oauth2:token:{tokenValue}", "Token index to authorization ID");

        structure.put("hashKeys", hashExample);
        structure.put("hashFields", hashFields);
        structure.put("tokenIndexes", tokenIndex);
        structure.put("advantages", java.util.List.of(
                "Fewer Redis keys",
                "Better memory efficiency",
                "Atomic hash operations",
                "Better data organization",
                "Easier bulk query"
        ));
        return Result.success(structure);
    }

    @Operation(summary = "Get blacklist statistics", description = "Get current token blacklist statistics")
    @GetMapping("/blacklist/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TokenBlacklistService.BlacklistStats> getBlacklistStats() {
        return Result.success(tokenBlacklistService.getBlacklistStats());
    }

    @Operation(summary = "Add token to blacklist", description = "Manually add a token to blacklist")
    @PostMapping("/blacklist/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> addToBlacklist(
            @Parameter(description = "Token value") @RequestParam String tokenValue,
            @Parameter(description = "Revocation reason") @RequestParam(defaultValue = "admin_manual") String reason) {
        OAuth2Authorization authorization = authorizationService.findByToken(tokenValue, null);
        String subject = authorization != null ? authorization.getPrincipalName() : "unknown";
        long ttlSeconds = 3600;
        tokenBlacklistService.addToBlacklist(tokenValue, subject, ttlSeconds, reason);
        return Result.success();
    }

    @Operation(summary = "Check blacklist status", description = "Check whether a token is blacklisted")
    @GetMapping("/blacklist/check")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> checkBlacklist(
            @Parameter(description = "Token value") @RequestParam String tokenValue) {
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(tokenValue);
        Map<String, Object> result = new HashMap<>();
        result.put("tokenValue", tokenValue.substring(0, Math.min(tokenValue.length(), 20)) + "...");
        result.put("isBlacklisted", isBlacklisted);
        result.put("checkTime", Instant.now());
        return Result.success(result);
    }

    @Operation(summary = "Cleanup blacklist entries", description = "Remove expired blacklist entries")
    @PostMapping("/blacklist/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> cleanupBlacklist() {
        int cleanedCount = tokenBlacklistService.cleanupExpiredEntries();
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("message", "Blacklist cleanup completed");
        result.put("cleanupTime", Instant.now());
        return Result.success(result);
    }
}
