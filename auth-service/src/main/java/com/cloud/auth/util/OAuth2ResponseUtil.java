package com.cloud.auth.util;

import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;






@Slf4j
@Component
public class OAuth2ResponseUtil {

    @Value("${app.jwt.test-mode:false}")
    private boolean testMode;

    @Value("${app.jwt.issuer:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String jwtIssuer;

    @Value("${app.jwt.access-token-validity:PT2H}")
    private String accessTokenValidity;

    @Value("${app.jwt.test-token-validity:P365D}")
    private String testTokenValidity;

    





    public static LoginResponseDTO buildSimpleLoginResponse(UserDTO userDTO) {
        LoginResponseDTO response = new LoginResponseDTO();

        
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        
        response.setToken_type("Bearer");
        response.setExpires_in(31536000L); 
        response.setScope("read write user.read user.write internal_api"); 

        return response;
    }

    






    public LoginResponseDTO buildLoginResponse(OAuth2Authorization authorization, UserDTO userDTO) {
        LoginResponseDTO response = new LoginResponseDTO();

        
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            response.setAccess_token(accessToken.getToken().getTokenValue());
            response.setToken_type(accessToken.getToken().getTokenType().getValue());

            
            Instant expiresAt = accessToken.getToken().getExpiresAt();
            if (expiresAt != null) {
                long expiresIn = Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
                response.setExpires_in(expiresIn);
            }

            
            Set<String> scopes = accessToken.getToken().getScopes();
            if (scopes != null && !scopes.isEmpty()) {
                response.setScope(String.join(" ", scopes));
            }
        }

        
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            response.setRefresh_token(refreshToken.getToken().getTokenValue());
        }

        
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }

    







    public LoginResponseDTO buildSimpleLoginResponse(UserDTO userDTO, JwtEncoder jwtEncoder) {
        LoginResponseDTO response = new LoginResponseDTO();

        
        if (userDTO != null && jwtEncoder != null) {
            
            Instant now = Instant.now();

            
            Instant expiresAt;
            long expiresInSeconds;

            if (testMode) {
                
                expiresAt = now.plus(365, ChronoUnit.DAYS);
                expiresInSeconds = 365 * 24 * 3600L; 
                
            } else {
                
                expiresAt = now.plus(2, ChronoUnit.HOURS);
                expiresInSeconds = 2 * 3600L; 
                
            }

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(jwtIssuer) 
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .subject(userDTO.getUsername())
                    .claim("username", userDTO.getUsername())
                    .claim("scope", "read write user.read user.write internal_api")
                    .claim("user_id", userDTO.getId())
                    .claim("user_type", userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null)
                    .claim("nickname", userDTO.getNickname())
                    .build();

            
            Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

            
            response.setAccess_token(jwt.getTokenValue());
            response.setToken_type("Bearer");
            response.setExpires_in(expiresInSeconds);
            response.setScope("read write user.read user.write internal_api");
        } else {
            
            response.setToken_type("Bearer");
            response.setExpires_in(31536000L); 
            response.setScope("read write user.read user.write internal_api"); 
        }

        
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }
}
