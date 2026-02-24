package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.enums.UserType;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private static final String DEFAULT_BCRYPT_PASSWORD =
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P3mTd.lQBHBR8y";

    private final UserFeignClient userFeignClient;

    @Autowired(required = false)
    private OAuth2ComplianceChecker complianceChecker;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("username", username, "username cannot be blank");
        }

        try {
            UserDTO userDTO = userFeignClient.findByUsername(username.trim());
            if (userDTO == null) {
                throw new ResourceNotFoundException("User", username);
            }
            if (userDTO.getStatus() != null && userDTO.getStatus() != 1) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }

            List<SimpleGrantedAuthority> authorities = buildUserAuthorities(userDTO.getUserType());
            String encodedPassword = getEncodedPassword(username);

            UserDetails userDetails = User.builder()
                    .username(userDTO.getUsername())
                    .password(encodedPassword)
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();

            if (complianceChecker != null) {
                try {
                    complianceChecker.validateCompliance(
                            userDetails,
                            userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null
                    );
                } catch (Exception e) {
                    log.debug("OAuth2 compliance check skipped: {}", e.getMessage());
                }
            }

            return userDetails;
        } catch (UsernameNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to load user details for {}", username, ex);
            throw new UsernameNotFoundException("Failed to load user details for " + username, ex);
        }
    }

    private List<SimpleGrantedAuthority> buildUserAuthorities(UserType userType) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_read"));

        if (userType == null) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_user:read"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_order:read"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_order:write"));
            return authorities;
        }

        switch (userType) {
            case ADMIN -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_admin:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_admin:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_user:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_user:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_merchant:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_merchant:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_product:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_product:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
            }
            case MERCHANT -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_merchant:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_merchant:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_product:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_product:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_stock:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_stock:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
            }
            case USER -> {
                authorities.add(new SimpleGrantedAuthority("SCOPE_user:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:read"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_order:write"));
                authorities.add(new SimpleGrantedAuthority("SCOPE_write"));
            }
        }
        return authorities;
    }

    private String getEncodedPassword(String username) {
        try {
            String encodedPassword = userFeignClient.getUserPassword(username);
            if (encodedPassword != null && !encodedPassword.trim().isEmpty() && !"null".equals(encodedPassword)) {
                return encodedPassword;
            }
        } catch (feign.FeignException.NotFound ex) {
            throw new ResourceNotFoundException("User password", username);
        } catch (Exception ex) {
            log.warn("Failed to load password from user-service for {}: {}", username, ex.getMessage());
        }
        return DEFAULT_BCRYPT_PASSWORD;
    }
}
