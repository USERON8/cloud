package com.cloud.auth.service;

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

    public boolean checkAccess(Authentication authentication, String resource, String action, Map<String, Object> context) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return false;
        }

        Long userId = jwt.getClaim("user_id");
        if (userId == null) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isMerchant = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()));
        boolean isUser = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }
        if (isMerchant) {
            return checkMerchantAccess(userId, resource, action, context);
        }
        if (isUser) {
            return checkUserAccess(userId, resource, action, context);
        }
        return false;
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

