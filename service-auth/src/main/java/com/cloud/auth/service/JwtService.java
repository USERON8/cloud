package com.cloud.auth.service;

import com.cloud.common.domain.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final TokenService tokenService;

    public JwtService(JwtEncoder jwtEncoder, TokenService tokenService) {
        this.jwtEncoder = jwtEncoder;
        this.tokenService = tokenService;
    }

    public String generateToken(Authentication authentication, UserDTO userDTO) {
        Instant now = Instant.now();

        // 使用UserDTO中的userType作为角色
        String roles = userDTO.getUserType();

        String token = getStringJWT(authentication, now, roles, userDTO.getId());

        // 将token存储到Redis中
        tokenService.storeToken(token, userDTO.getId());

        return token;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        // 默认角色设置为USER
        String roles = "USER";

        return getStringJWT(authentication, now, roles, null);
    }

    private String getStringJWT(Authentication authentication, Instant now, String roles, Long userId) {
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("http://localhost:8082")
                .subject(authentication.getName())
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS));

        // 如果用户ID存在，添加到声明中
        if (userId != null) {
            claimsBuilder.claim("userId", userId.toString());
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);

        return jwtEncoder.encode(parameters).getTokenValue();
    }

    public long getExpirationTime() {
        return 3600000; // 1小时
    }
}