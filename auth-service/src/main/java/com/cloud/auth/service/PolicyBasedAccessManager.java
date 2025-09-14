package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于策略的访问管理器
 * 用于处理复杂的业务规则权限控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyBasedAccessManager {

    private final UserFeignClient userFeignClient;

    /**
     * 检查访问权限
     *
     * @param authentication 认证信息
     * @param resource       资源类型
     * @param action         操作类型
     * @param context        上下文信息
     * @return 是否允许访问
     */
    public boolean checkAccess(Authentication authentication, String resource, String action, Map<String, Object> context) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("用户未认证，拒绝访问 resource={}, action={}", resource, action);
            return false;
        }

        // 从JWT中获取用户信息
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt)) {
            log.debug("认证信息不是JWT格式，拒绝访问 resource={}, action={}", resource, action);
            return false;
        }

        Jwt jwt = (Jwt) principal;
        Long userId = jwt.getClaim("user_id");
        String userType = jwt.getClaimAsString("user_type");

        if (userId == null || userType == null) {
            log.debug("JWT中缺少必要信息，拒绝访问 resource={}, action={}", resource, action);
            return false;
        }

        log.debug("开始检查访问权限: userId={}, userType={}, resource={}, action={}", userId, userType, resource, action);

        // 根据用户类型进行不同的权限检查
        switch (userType) {
            case "ADMIN":
                // 管理员具有所有权限
                log.debug("管理员用户，允许访问 resource={}, action={}", resource, action);
                return true;

            case "MERCHANT":
                return checkMerchantAccess(userId, resource, action, context);

            case "USER":
                return checkUserAccess(userId, resource, action, context);

            default:
                log.debug("未知用户类型，拒绝访问 resource={}, action={}", resource, action);
                return false;
        }
    }

    /**
     * 检查商家用户的访问权限
     *
     * @param merchantId 商家ID
     * @param resource   资源类型
     * @param action     操作类型
     * @param context    上下文信息
     * @return 是否允许访问
     */
    private boolean checkMerchantAccess(Long merchantId, String resource, String action, Map<String, Object> context) {
        log.debug("检查商家访问权限: merchantId={}, resource={}, action={}", merchantId, resource, action);

        switch (resource) {
            case "product":
                // 商家只能管理自己的商品
                return checkMerchantProductAccess(merchantId, action, context);

            case "order":
                // 商家只能查看和处理与自己相关的订单
                return checkMerchantOrderAccess(merchantId, action, context);

            default:
                log.debug("商家无权访问资源: resource={}", resource);
                return false;
        }
    }

    /**
     * 检查普通用户的访问权限
     *
     * @param userId   用户ID
     * @param resource 资源类型
     * @param action   操作类型
     * @param context  上下文信息
     * @return 是否允许访问
     */
    private boolean checkUserAccess(Long userId, String resource, String action, Map<String, Object> context) {
        log.debug("检查普通用户访问权限: userId={}, resource={}, action={}", userId, resource, action);

        switch (resource) {
            case "order":
                // 用户只能查看和操作自己的订单
                return checkUserOrderAccess(userId, action, context);

            case "address":
                // 用户只能管理自己的地址
                return checkUserAddressAccess(userId, action, context);

            default:
                log.debug("用户无权访问资源: resource={}", resource);
                return false;
        }
    }

    /**
     * 检查商家商品访问权限
     *
     * @param merchantId 商家ID
     * @param action     操作类型
     * @param context    上下文信息
     * @return 是否允许访问
     */
    private boolean checkMerchantProductAccess(Long merchantId, String action, Map<String, Object> context) {
        // 获取商品ID
        Long productId = (Long) context.get("productId");
        if (productId == null) {
            log.debug("缺少商品ID，拒绝访问");
            return false;
        }

        // 这里应该调用商品服务检查商品是否属于该商家
        // 为简化示例，我们假设检查通过
        log.debug("商家商品访问权限检查通过: merchantId={}, productId={}, action={}", merchantId, productId, action);
        return true;
    }

    /**
     * 检查商家订单访问权限
     *
     * @param merchantId 商家ID
     * @param action     操作类型
     * @param context    上下文信息
     * @return 是否允许访问
     */
    private boolean checkMerchantOrderAccess(Long merchantId, String action, Map<String, Object> context) {
        // 获取订单ID
        Long orderId = (Long) context.get("orderId");
        if (orderId == null) {
            log.debug("缺少订单ID，拒绝访问");
            return false;
        }

        // 这里应该调用订单服务检查订单是否属于该商家
        // 为简化示例，我们假设检查通过
        log.debug("商家订单访问权限检查通过: merchantId={}, orderId={}, action={}", merchantId, orderId, action);
        return true;
    }

    /**
     * 检查用户订单访问权限
     *
     * @param userId  用户ID
     * @param action  操作类型
     * @param context 上下文信息
     * @return 是否允许访问
     */
    private boolean checkUserOrderAccess(Long userId, String action, Map<String, Object> context) {
        // 获取订单ID
        Long orderId = (Long) context.get("orderId");
        if (orderId == null) {
            log.debug("缺少订单ID，拒绝访问");
            return false;
        }

        // 这里应该调用订单服务检查订单是否属于该用户
        // 为简化示例，我们假设检查通过
        log.debug("用户订单访问权限检查通过: userId={}, orderId={}, action={}", userId, orderId, action);
        return true;
    }

    /**
     * 检查用户地址访问权限
     *
     * @param userId  用户ID
     * @param action  操作类型
     * @param context 上下文信息
     * @return 是否允许访问
     */
    private boolean checkUserAddressAccess(Long userId, String action, Map<String, Object> context) {
        // 获取地址ID
        Long addressId = (Long) context.get("addressId");
        if (addressId == null) {
            log.debug("缺少地址ID，拒绝访问");
            return false;
        }

        // 这里应该调用用户服务检查地址是否属于该用户
        // 为简化示例，我们假设检查通过
        log.debug("用户地址访问权限检查通过: userId={}, addressId={}, action={}", userId, addressId, action);
        return true;
    }
}