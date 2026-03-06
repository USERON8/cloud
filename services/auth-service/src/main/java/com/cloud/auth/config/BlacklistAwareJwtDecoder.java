package com.cloud.auth.config;

import com.cloud.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;







@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Jwt decode(String token) throws JwtException {
        
        Jwt jwt = delegate.decode(token);

        
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            log.warn("WT: subject={}, jti={}",
                    jwt.getSubject(), jwt.getClaimAsString("jti"));
            throw new JwtValidationException("JWT token has been revoked",
                    OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null)).getErrors());
        }

        return jwt;
    }

    



    @Component
    @RequiredArgsConstructor
    public static class BlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

        private final TokenBlacklistService tokenBlacklistService;

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.debug("JWT, subject={}", jwt.getSubject());
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null));
            }

            return OAuth2TokenValidatorResult.success();
        }
    }
}
