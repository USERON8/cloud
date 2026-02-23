package com.cloud.auth.config;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;




@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserFeignClient userFeignClient;

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }

        context.getClaims()
                .claim("client_id", context.getRegisteredClient().getClientId())
                .claim("token_version", "v1");

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            context.getClaims().claim("user_type", "SERVICE");
        }

        Object principal = context.getPrincipal().getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();
            context.getClaims().claim("username", username);

            try {
                UserDTO user = userFeignClient.findByUsername(username);
                if (user != null) {
                    String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
                    context.getClaims()
                            .claim("user_id", user.getId())
                            .claim("user_type", userTypeCode)
                            .claim("nickname", user.getNickname())
                            .claim("status", user.getStatus());
                }
            } catch (Exception ex) {
                
                log.warn("load user info for jwt claims failed, username={}", username, ex);
            }
        } else {
            String principalName = context.getPrincipal().getName();
            if (principalName != null && !principalName.isBlank()) {
                context.getClaims().claim("username", principalName);
                try {
                    UserDTO user = userFeignClient.findByUsername(principalName);
                    if (user != null) {
                        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
                        context.getClaims()
                                .claim("user_id", user.getId())
                                .claim("user_type", userTypeCode)
                                .claim("nickname", user.getNickname())
                                .claim("status", user.getStatus());
                    }
                } catch (Exception ex) {
                    log.warn("load user info for jwt claims failed, username={}", principalName, ex);
                }
            }
        }
    }
}
