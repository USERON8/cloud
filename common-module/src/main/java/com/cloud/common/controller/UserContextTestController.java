package com.cloud.common.controller;

import com.cloud.common.utils.UserContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户上下文测试控制器
 * 用于测试JWT token转发和用户信息提取
 * 
 * 注意：这是一个示例控制器，各个服务可以参考这个实现
 * 实际使用时应该根据业务需求创建具体的控制器
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/test/user-context")
@Tag(name = "用户上下文测试", description = "测试JWT token转发和用户信息提取功能")
public class UserContextTestController {

    /**
     * 获取当前用户的基本信息
     * 测试Gateway转发的JWT token解析
     */
    @GetMapping("/current-user")
    @Operation(summary = "获取当前用户信息", description = "从JWT token和HTTP头中提取用户信息")
    @PreAuthorize("hasAnyAuthority('SCOPE_read', 'SCOPE_user.read')")
    public Map<String, Object> getCurrentUser() {
        log.info("🔍 开始获取当前用户信息");
        
        Map<String, Object> userInfo = new LinkedHashMap<>();
        
        // 基本用户信息
        userInfo.put("userId", UserContextUtils.getCurrentUserId());
        userInfo.put("username", UserContextUtils.getCurrentUsername());
        userInfo.put("userType", UserContextUtils.getCurrentUserType());
        userInfo.put("nickname", UserContextUtils.getCurrentUserNickname());
        userInfo.put("status", UserContextUtils.getCurrentUserStatus());
        userInfo.put("phone", UserContextUtils.getCurrentUserPhone());
        
        // 客户端和Token信息
        userInfo.put("clientId", UserContextUtils.getClientId());
        userInfo.put("tokenVersion", UserContextUtils.getTokenVersion());
        
        // 权限信息
        Set<String> scopes = UserContextUtils.getCurrentUserScopes();
        userInfo.put("scopes", scopes);
        
        // 认证状态
        userInfo.put("isAuthenticated", UserContextUtils.isAuthenticated());
        
        // 用户类型判断
        userInfo.put("isRegularUser", UserContextUtils.isRegularUser());
        userInfo.put("isMerchant", UserContextUtils.isMerchant());
        userInfo.put("isAdmin", UserContextUtils.isAdmin());
        
        log.info("✅ 成功获取用户信息: {}", UserContextUtils.getCurrentUserInfo());
        
        return userInfo;
    }

    /**
     * 获取原始JWT token
     * 用于测试token转发是否正常
     */
    @GetMapping("/jwt-token")
    @Operation(summary = "获取JWT Token", description = "获取原始的JWT token字符串")
    @PreAuthorize("hasAnyAuthority('SCOPE_read', 'SCOPE_write')")
    public Map<String, Object> getJwtToken() {
        log.info("🔑 开始获取JWT Token信息");
        
        Map<String, Object> tokenInfo = new LinkedHashMap<>();
        
        String token = UserContextUtils.getCurrentToken();
        if (token != null) {
            // 只显示token的前20个字符和后10个字符，中间用...代替（安全考虑）
            String maskedToken = token.length() > 30 ? 
                token.substring(0, 20) + "..." + token.substring(token.length() - 10) : token;
            tokenInfo.put("tokenPreview", maskedToken);
            tokenInfo.put("tokenLength", token.length());
        } else {
            tokenInfo.put("tokenPreview", null);
            tokenInfo.put("tokenLength", 0);
        }
        
        tokenInfo.put("hasToken", token != null);
        
        log.info("✅ JWT Token信息获取完成");
        
        return tokenInfo;
    }

    /**
     * 测试权限检查
     * 验证scope权限是否正确传递和解析
     */
    @GetMapping("/test-permissions")
    @Operation(summary = "测试权限检查", description = "测试不同scope权限的检查")
    public Map<String, Object> testPermissions() {
        log.info("🔐 开始测试权限检查");
        
        Map<String, Object> permissions = new LinkedHashMap<>();
        
        // 测试各种scope权限
        permissions.put("hasReadScope", UserContextUtils.hasScope("read"));
        permissions.put("hasWriteScope", UserContextUtils.hasScope("write"));
        permissions.put("hasUserReadScope", UserContextUtils.hasScope("user.read"));
        permissions.put("hasUserWriteScope", UserContextUtils.hasScope("user.write"));
        permissions.put("hasInternalApiScope", UserContextUtils.hasScope("internal_api"));
        
        // 测试复合权限检查
        permissions.put("hasAnyUserScope", UserContextUtils.hasAnyScope("user.read", "user.write"));
        permissions.put("hasAnyAdminScope", UserContextUtils.hasAnyScope("admin.read", "admin.write"));
        
        // 当前用户的所有权限
        permissions.put("allScopes", UserContextUtils.getCurrentUserScopes());
        
        log.info("✅ 权限检查测试完成: {}", permissions);
        
        return permissions;
    }

    /**
     * 测试HTTP头信息获取
     * 验证Gateway转发的自定义头是否正确接收
     */
    @GetMapping("/test-headers")
    @Operation(summary = "测试HTTP头信息", description = "测试Gateway转发的自定义HTTP头")
    public Map<String, Object> testHeaders() {
        log.info("📋 开始测试HTTP头信息");
        
        Map<String, Object> headers = new LinkedHashMap<>();
        
        // 获取Gateway转发的用户信息头（不包含敏感信息）
        headers.put("X-User-Id", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_ID));
        headers.put("X-User-Name", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_NAME));
        headers.put("X-User-Type", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_TYPE));
        headers.put("X-User-Nickname", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_NICKNAME));
        headers.put("X-User-Status", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_STATUS));
        headers.put("X-Client-Id", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_CLIENT_ID));
        headers.put("X-Token-Version", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_TOKEN_VERSION));
        headers.put("X-User-Scopes", UserContextUtils.getHeaderValue(UserContextUtils.HEADER_USER_SCOPES));
        
        // 注意：敏感信息如手机号不会通过HTTP头传递，仅从JWT token中获取
        headers.put("phoneFromJWT", UserContextUtils.getCurrentUserPhone());
        
        log.info("✅ HTTP头信息测试完成");
        
        return headers;
    }

    /**
     * 管理员专用接口
     * 测试用户类型权限控制
     */
    @GetMapping("/admin-only")
    @Operation(summary = "管理员专用接口", description = "仅管理员可访问，测试用户类型判断")
    @PreAuthorize("hasAnyAuthority('SCOPE_admin.read', 'ROLE_ADMIN')")
    public Map<String, Object> adminOnly() {
        log.info("👑 管理员接口被调用");
        
        Map<String, Object> adminInfo = new LinkedHashMap<>();
        adminInfo.put("message", "欢迎管理员！");
        adminInfo.put("currentUser", UserContextUtils.getCurrentUsername());
        adminInfo.put("userType", UserContextUtils.getCurrentUserType());
        adminInfo.put("isAdmin", UserContextUtils.isAdmin());
        adminInfo.put("timestamp", System.currentTimeMillis());
        
        return adminInfo;
    }

    /**
     * 商户专用接口
     * 测试商户权限控制
     */
    @GetMapping("/merchant-only")
    @Operation(summary = "商户专用接口", description = "仅商户可访问，测试商户类型判断")
    public Map<String, Object> merchantOnly() {
        // 手动检查是否为商户
        if (!UserContextUtils.isMerchant()) {
            throw new SecurityException("仅商户可访问此接口");
        }
        
        log.info("🏪 商户接口被调用");
        
        Map<String, Object> merchantInfo = new LinkedHashMap<>();
        merchantInfo.put("message", "欢迎商户！");
        merchantInfo.put("currentUser", UserContextUtils.getCurrentUsername());
        merchantInfo.put("userType", UserContextUtils.getCurrentUserType());
        merchantInfo.put("isMerchant", UserContextUtils.isMerchant());
        merchantInfo.put("timestamp", System.currentTimeMillis());
        
        return merchantInfo;
    }
}
