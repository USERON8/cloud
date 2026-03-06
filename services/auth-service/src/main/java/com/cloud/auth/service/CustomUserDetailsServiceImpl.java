package com.cloud.auth.service;

import com.cloud.auth.module.entity.AuthUser;
import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final LocalUserAuthorityService localUserAuthorityService;
    private final AuthIdentityService authIdentityService;

    @Autowired(required = false)
    private OAuth2ComplianceChecker complianceChecker;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("username", username, "username cannot be blank");
        }

        try {
            AuthUser authUser = authIdentityService.findByUsername(username.trim());
            if (authUser == null) {
                throw new ResourceNotFoundException("User", username);
            }
            if (authUser.getStatus() != null && authUser.getStatus() != 1) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }

            List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                    localUserAuthorityService.buildAuthorities(authIdentityService.getRoleCodes(authUser.getId()));

            UserDetails userDetails = User.builder()
                    .username(authUser.getUsername())
                    .password(authUser.getPassword())
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();

            if (complianceChecker != null) {
                try {
                    complianceChecker.validateCompliance(userDetails, authIdentityService.getRoleCodes(authUser.getId()));
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
}

