package com.cloud.auth.service;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.enums.UserType;
import com.cloud.common.exception.BusinessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocalUserAuthorityService {

    public List<SimpleGrantedAuthority> buildAuthorities(UserType userType) {
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

    public Authentication createAuthenticatedPrincipal(UserDTO userDTO) {
        if (userDTO == null) {
            throw new IllegalArgumentException("User DTO cannot be null");
        }
        if (userDTO.getStatus() != null && userDTO.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        List<SimpleGrantedAuthority> authorities = buildAuthorities(userDTO.getUserType());
        UserDetails userDetails = User.builder()
                .username(userDTO.getUsername())
                .password("[SOCIAL_LOGIN]")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();

        return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, authorities);
    }
}
