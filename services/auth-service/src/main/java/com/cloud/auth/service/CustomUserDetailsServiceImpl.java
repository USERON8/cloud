package com.cloud.auth.service;

import com.cloud.api.user.UserDubboApi;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    @DubboReference(check = false, timeout = 5000, retries = 0)
    private UserDubboApi userDubboApi;
    private final LocalUserAuthorityService localUserAuthorityService;

    @Autowired(required = false)
    private OAuth2ComplianceChecker complianceChecker;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("username", username, "username cannot be blank");
        }

        try {
            UserDTO userDTO = userDubboApi.findByUsername(username.trim());
            if (userDTO == null) {
                throw new ResourceNotFoundException("User", username);
            }
            if (userDTO.getStatus() != null && userDTO.getStatus() != 1) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }

            List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                    localUserAuthorityService.buildAuthorities(userDTO.getRoles());
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
                    complianceChecker.validateCompliance(userDetails, userDTO.getRoles());
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

    private String getEncodedPassword(String username) {
        try {
            String encodedPassword = userDubboApi.getUserPassword(username);
            if (encodedPassword != null && !encodedPassword.trim().isEmpty() && !"null".equals(encodedPassword)) {
                return encodedPassword;
            }
        } catch (Exception ex) {
            log.warn("Failed to load password from user-service for {}: {}", username, ex.getMessage());
        }
        throw new ResourceNotFoundException("User password", username);
    }
}

