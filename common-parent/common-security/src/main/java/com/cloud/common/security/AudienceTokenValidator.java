package com.cloud.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

public class AudienceTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> acceptedAudiences;

    public AudienceTokenValidator(Set<String> acceptedAudiences) {
        this.acceptedAudiences = acceptedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (acceptedAudiences == null || acceptedAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }
        if (jwt.getAudience() == null || jwt.getAudience().isEmpty()) {
            return failure("invalid_audience", "JWT audience is missing");
        }
        boolean matched = jwt.getAudience().stream().anyMatch(acceptedAudiences::contains);
        return matched ? OAuth2TokenValidatorResult.success()
                : failure("invalid_audience", "JWT audience is not accepted");
    }

    private OAuth2TokenValidatorResult failure(String code, String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
    }
}
