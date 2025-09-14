package com.cloud.product.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 店铺权限验证辅助类
 * 提供店铺相关的权限验证功能
 *
 * @author what's up
 */
@Slf4j
@Component
public class ShopPermissionHelper {

    // 权限常量
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MERCHANT = "ROLE_MERCHANT";

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(ROLE_ADMIN));
    }

    /**
     * 检查是否为商家
     */
    public boolean isMerchant(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(ROLE_MERCHANT));
    }

    /**
     * 检查是否为店铺所有者（商家ID匹配）
     */
    public boolean isShopOwner(Authentication authentication, Long merchantId) {
        if (authentication == null || merchantId == null) {
            return false;
        }

        // 从认证信息中获取用户ID（这里假设principal是用户ID）
        try {
            Long userId = Long.valueOf(authentication.getName());
            return userId.equals(merchantId);
        } catch (NumberFormatException e) {
            log.warn("无法解析用户ID: {}", authentication.getName());
            return false;
        }
    }

    /**
     * 检查用户是否有权限访问店铺（管理员或店铺所有者）
     */
    public boolean hasShopPermission(Authentication authentication, Long merchantId) {
        return isAdmin(authentication) || isShopOwner(authentication, merchantId);
    }

    /**
     * 检查是否为系统用户（管理员或商家）
     */
    public boolean isSystemUser(Authentication authentication) {
        return isAdmin(authentication) || isMerchant(authentication);
    }

    /**
     * 获取当前用户ID
     */
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            log.warn("无法解析用户ID: {}", authentication.getName());
            return null;
        }
    }

    /**
     * 验证店铺查询权限
     * 管理员可以查询所有店铺，商家只能查询自己的店铺
     */
    public boolean canViewShop(Authentication authentication, Long merchantId) {
        return hasShopPermission(authentication, merchantId);
    }

    /**
     * 验证店铺管理权限
     * 管理员可以管理所有店铺，商家只能管理自己的店铺
     */
    public boolean canManageShop(Authentication authentication, Long merchantId) {
        return hasShopPermission(authentication, merchantId);
    }

    /**
     * 验证店铺删除权限
     * 只有管理员可以删除店铺
     */
    public boolean canDeleteShop(Authentication authentication) {
        return isAdmin(authentication);
    }
}
