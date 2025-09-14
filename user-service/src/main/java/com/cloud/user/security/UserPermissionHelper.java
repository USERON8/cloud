package com.cloud.user.security;

import com.cloud.common.utils.SecurityContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 用户权限辅助工具类
 * 提供方法级权限检查的具体实现
 *
 * @author what's up
 */
@Slf4j
@Component
public class UserPermissionHelper {

    /**
     * 检查用户是否为管理员
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @return 是否为管理员
     */
    public boolean isAdmin(Authentication authentication) {
        return SecurityContextUtils.hasRole("ADMIN");
    }

    /**
     * 检查用户是否为商户
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @return 是否为商户
     */
    public boolean isMerchant(Authentication authentication) {
        return SecurityContextUtils.hasRole("MERCHANT");
    }

    /**
     * 检查当前认证用户是否为数据所有者
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @param userId         要检查的用户ID
     * @return 是否为数据所有者
     */
    public boolean isOwner(Authentication authentication, Long userId) {
        if (userId == null) {
            return false;
        }

        return SecurityContextUtils.isSameUser(userId.toString());
    }

    /**
     * 检查当前认证用户是否为商户所有者
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @param merchantId     要检查的商户ID
     * @return 是否为商户所有者
     */
    public boolean isMerchantOwner(Authentication authentication, Long merchantId) {
        if (merchantId == null) {
            return false;
        }

        try {
            // 如果是管理员，直接返回true
            if (SecurityContextUtils.hasRole("ADMIN")) {
                return true;
            }

            // 检查JWT中的商户ID claim
            String merchantIdClaim = SecurityContextUtils.getClaim("merchant_id", String.class);
            if (merchantIdClaim != null) {
                return merchantIdClaim.equals(merchantId.toString());
            }

            return false;
        } catch (Exception e) {
            log.warn("检查商户所有权时发生异常", e);
            return false;
        }
    }

    /**
     * 检查用户是否有特定资源的访问权限
     *
     * @param authentication Spring Security认证对象
     * @param resourceId     资源ID
     * @param resourceType   资源类型
     * @return 是否有访问权限
     */
    public boolean hasResourceAccess(Authentication authentication, String resourceId, String resourceType) {
        if (authentication == null || !authentication.isAuthenticated() ||
                resourceId == null || resourceType == null) {
            return false;
        }

        try {
            // 管理员拥有所有资源的访问权限
            if (isAdmin(authentication)) {
                return true;
            }

            // 根据资源类型进行不同的权限检查
            return switch (resourceType.toLowerCase()) {
                case "user" -> isOwner(authentication, Long.valueOf(resourceId));
                case "merchant" -> isMerchantOwner(authentication, Long.valueOf(resourceId));
                case "address" -> checkAddressAccess(authentication, resourceId);
                default -> {
                    log.warn("未知的资源类型: {}", resourceType);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.warn("检查资源访问权限时发生异常", e);
            return false;
        }
    }


    /**
     * 检查地址访问权限
     *
     * @param authentication Spring Security认证对象
     * @param addressId      地址ID
     * @return 是否有访问权限
     */
    private boolean checkAddressAccess(Authentication authentication, String addressId) {
        // 这里需要根据实际业务逻辑实现
        // 通常需要查询地址表，确认地址归属的用户ID是否与当前用户一致
        // 为了简化示例，这里暂时返回true，实际项目中需要实现具体的业务逻辑

        log.debug("检查地址访问权限: addressId={}, userId={}",
                addressId, SecurityContextUtils.getCurrentUserId());

        // TODO: 实现具体的地址访问权限检查逻辑
        // 可能需要注入AddressService来查询地址归属关系
        return true;
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @param authority      权限名称
     * @return 是否拥有权限
     */
    public boolean hasAuthority(Authentication authentication, String authority) {
        return SecurityContextUtils.getAllAuthorities().contains(authority);
    }

    /**
     * 检查JWT token是否有效且未过期
     *
     * @param authentication Spring Security认证对象（为了兼容现有调用，实际不使用）
     * @return token是否有效
     */
    public boolean isTokenValid(Authentication authentication) {
        if (!SecurityContextUtils.isAuthenticated()) {
            return false;
        }

        try {
            var jwt = SecurityContextUtils.getCurrentJwt();
            if (jwt != null && jwt.getExpiresAt() != null) {
                return jwt.getExpiresAt().isAfter(java.time.Instant.now());
            }
        } catch (Exception e) {
            log.warn("检查JWT token有效性时发生异常", e);
            return false;
        }

        return SecurityContextUtils.isAuthenticated();
    }
}
