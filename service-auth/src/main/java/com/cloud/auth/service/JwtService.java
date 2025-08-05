package com.cloud.auth.service;

import com.cloud.common.domain.dto.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    
    @Value("${jwt.secret:my-secret-key-for-development-only-should-be-changed-in-production}")
    private String jwtSecret;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(Authentication authentication, UserDTO userDTO) {
        Instant now = Instant.now();

        // 使用UserDTO中的userType作为角色
        String roles = userDTO.getUserType();

        return getStringJWT(authentication, now, roles);
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        // 默认角色设置为USER
        String roles = "USER";

        return getStringJWT(authentication, now, roles);
    }

    private String getStringJWT(Authentication authentication, Instant now, String roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://auth-service:8001")
                .subject(authentication.getName())
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        return jwtEncoder.encode(parameters).getTokenValue();
    }


    public long getExpirationTime() {
        return 3600000;
    }
}