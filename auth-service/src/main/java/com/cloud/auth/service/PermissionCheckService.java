package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;





@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final UserFeignClient userFeignClient;

    






    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    






    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    





    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    





    public boolean isMerchant(Authentication authentication) {
        return hasRole(authentication, "ROLE_MERCHANT");
    }

    





    public boolean isUser(Authentication authentication) {
        return hasRole(authentication, "ROLE_USER");
    }

    







    public boolean canAccessResource(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        
        if (isAdmin(authentication)) {
            return true;
        }

        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String userType = jwt.getClaimAsString("user_type");
            Long userId = jwt.getClaim("user_id");

            
            switch (userType) {
                case "ADMIN":
                    return true;
                case "MERCHANT":
                    
                    return isMerchantResource(resource, userId);
                case "USER":
                    
                    return isUserResource(resource, userId);
                default:
                    return false;
            }
        }

        return false;
    }

    






    private boolean isMerchantResource(String resource, Long merchantId) {
        
        
        return true;
    }

    






    private boolean isUserResource(String resource, Long userId) {
        
        
        return true;
    }

    





    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return jwt.getClaim("user_id");
        }

        return null;
    }

    





    public String getCurrentUserType(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return jwt.getClaimAsString("user_type");
        }

        return null;
    }

    





    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String username = jwt.getSubject();
            try {
                return userFeignClient.findByUsername(username);
            } catch (Exception e) {
                log.error("鑾峰彇鐢ㄦ埛淇℃伅澶辫触: username={}", username, e);
                return null;
            }
        }

        return null;
    }
}
