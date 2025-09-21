package com.cloud.auth.controller;

import com.cloud.auth.service.SimpleRedisHashOAuth2AuthorizationService;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 Token管理控制器
 * 提供基于Redis Hash存储的token管理功能
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/oauth2/manage")
@RequiredArgsConstructor
@Tag(name = "🔑 OAuth2 Token管理", description = "OAuth2 Token管理和监控接口")
public class OAuth2TokenManageController {

    private final OAuth2AuthorizationService authorizationService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查看token存储统计信息
     */
    @Operation(summary = "获取token存储统计", description = "查看当前Redis中存储的token统计信息")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getTokenStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 统计授权信息Hash的数量
            Set<String> authKeys = redisTemplate.keys("oauth2:auth:*");
            stats.put("authorizationCount", authKeys != null ? authKeys.size() : 0);

            // 统计token索引的数量  
            Set<String> tokenKeys = redisTemplate.keys("oauth2:token:*");
            stats.put("tokenIndexCount", tokenKeys != null ? tokenKeys.size() : 0);

            // Redis内存使用情况（需要Redis INFO命令支持）
            stats.put("redisInfo", "Hash存储模式");
            stats.put("storageType", "Redis Hash");

            // 获取特定统计信息（如果使用SimpleRedisHashOAuth2AuthorizationService）
            if (authorizationService instanceof SimpleRedisHashOAuth2AuthorizationService hashService) {
                SimpleRedisHashOAuth2AuthorizationService.TokenStorageStats serviceStats =
                        hashService.getStorageStats();
                stats.put("serviceStats", serviceStats);
            }

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取token统计信息失败", e);
            return Result.error("获取统计信息失败");
        }
    }

    /**
     * 查看指定授权信息详情
     */
    @Operation(summary = "查看授权详情", description = "根据授权ID查看详细的授权信息")
    @GetMapping("/authorization/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAuthorizationDetails(
            @Parameter(description = "授权ID") @PathVariable String id) {
        try {
            OAuth2Authorization authorization = authorizationService.findById(id);

            if (authorization == null) {
                return Result.error("授权信息不存在");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("id", authorization.getId());
            details.put("clientId", authorization.getRegisteredClientId());
            details.put("principalName", authorization.getPrincipalName());
            details.put("grantType", authorization.getAuthorizationGrantType().getValue());
            details.put("scopes", authorization.getAuthorizedScopes());

            // Token信息（不显示完整token值，只显示状态）
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

        } catch (Exception e) {
            log.error("查看授权详情失败: {}", id, e);
            return Result.error("查看授权详情失败");
        }
    }

    /**
     * 撤销指定的授权
     */
    @Operation(summary = "撤销授权", description = "撤销指定ID的OAuth2授权")
    @DeleteMapping("/authorization/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> revokeAuthorization(
            @Parameter(description = "授权ID") @PathVariable String id) {
        try {
            OAuth2Authorization authorization = authorizationService.findById(id);

            if (authorization == null) {
                return Result.error("授权信息不存在");
            }

            authorizationService.remove(authorization);
            log.info("管理员撤销授权: id={}, clientId={}, principalName={}",
                    id, authorization.getRegisteredClientId(), authorization.getPrincipalName());

            return Result.success();

        } catch (Exception e) {
            log.error("撤销授权失败: {}", id, e);
            return Result.error("撤销授权失败");
        }
    }

    /**
     * 清理过期token（维护操作）
     */
    @Operation(summary = "清理过期token", description = "手动触发过期token清理（通常由Redis TTL自动处理）")
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> cleanupExpiredTokens() {
        try {
            if (authorizationService instanceof SimpleRedisHashOAuth2AuthorizationService hashService) {
                hashService.cleanupExpiredTokens();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "清理任务已执行");
            result.put("note", "Redis TTL机制会自动清理过期数据");

            return Result.success(result);

        } catch (Exception e) {
            log.error("清理过期token失败", e);
            return Result.error("清理操作失败");
        }
    }

    /**
     * 查看Redis Hash存储结构示例
     */
    @Operation(summary = "Hash存储结构示例", description = "展示Redis Hash存储OAuth2授权信息的结构")
    @GetMapping("/storage-structure")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStorageStructure() {
        Map<String, Object> structure = new HashMap<>();

        // Hash存储结构示例
        Map<String, String> hashExample = new HashMap<>();
        hashExample.put("oauth2:auth:{authorizationId}", "Redis Hash Key - 存储完整的授权信息");

        Map<String, String> hashFields = new HashMap<>();
        hashFields.put("data", "完整的OAuth2Authorization JSON序列化数据");
        hashFields.put("clientId", "客户端ID（便于查询）");
        hashFields.put("principalName", "用户名（便于查询）");
        hashFields.put("createTime", "创建时间");

        Map<String, String> tokenIndex = new HashMap<>();
        tokenIndex.put("oauth2:token:{tokenValue}", "Token索引 -> authorizationId");

        structure.put("hashKeys", hashExample);
        structure.put("hashFields", hashFields);
        structure.put("tokenIndexes", tokenIndex);
        structure.put("advantages", java.util.List.of(
                "减少Redis key数量",
                "提高内存效率",
                "支持原子操作",
                "更好的数据组织",
                "便于批量查询"
        ));

        return Result.success(structure);
    }
}
