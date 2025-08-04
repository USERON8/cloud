package com.cloud.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    public String generateToken(Authentication authentication) {
        return Jwts.builder()
                .subject(authentication.getName())
                .issuer("http://auth-service:8001") // JWT 发行方
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时
                .signWith(key)
                .compact();
    }

    public SecretKey getKey() {
        return key;
    }

    public long getExpirationTime() {
        return 3600000;
    }
}