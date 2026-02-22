package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyBasedAccessManager {

    private final UserFeignClient userFeignClient;

    public boolean checkAccess(Authentication authentication, String resource, String action, Map<String, Object> context) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return false;
        }

        Long userId = jwt.getClaim("user_id");
        String userType = jwt.getClaimAsString("user_type");
        if (userId == null || userType == null) {
            return false;
        }

        return switch (userType) {
            case "ADMIN" -> true;
            case "MERCHANT" -> checkMerchantAccess(userId, resource, action, context);
            case "USER" -> checkUserAccess(userId, resource, action, context);
            default -> false;
        };
    }

    private boolean checkMerchantAccess(Long merchantId, String resource, String action, Map<String, Object> context) {
        return switch (resource) {
            case "product" -> checkMerchantProductAccess(merchantId, action, context);
            case "order" -> checkMerchantOrderAccess(merchantId, action, context);
            default -> false;
        };
    }

    private boolean checkUserAccess(Long userId, String resource, String action, Map<String, Object> context) {
        return switch (resource) {
            case "order" -> checkUserOrderAccess(userId, action, context);
            case "address" -> checkUserAddressAccess(userId, action, context);
            default -> false;
        };
    }

    private boolean checkMerchantProductAccess(Long merchantId, String action, Map<String, Object> context) {
        Long productId = (Long) context.get("productId");
        return productId != null;
    }

    private boolean checkMerchantOrderAccess(Long merchantId, String action, Map<String, Object> context) {
        Long orderId = (Long) context.get("orderId");
        return orderId != null;
    }

    private boolean checkUserOrderAccess(Long userId, String action, Map<String, Object> context) {
        Long orderId = (Long) context.get("orderId");
        return orderId != null;
    }

    private boolean checkUserAddressAccess(Long userId, String action, Map<String, Object> context) {
        Long addressId = (Long) context.get("addressId");
        return addressId != null;
    }
}
