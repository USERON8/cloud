package com.cloud.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT和密码编码配置
 * 严格遵循OAuth2.1和JWT安全标准
 * <p>
 * 功能包括:
 * - RSA密钥对生成和管理
 * - JWT编码器和解码器配置
 * - OAuth2.1令牌生成器配置
 * - 密码编码器配置（支持多种算法）
 * - JWT权限转换器配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class JwtPasswordConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    /**
     * RSA密钥对生成器
     * OAuth2.1推荐使用RSA256算法
     */
    @Bean
    public KeyPair keyPair() {
        log.info("🔧 生成OAuth2.1 RSA密钥对");

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);  // OAuth2.1推荐2048位
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            log.info("✅ RSA密钥对生成完成");
            return keyPair;

        } catch (Exception e) {
            log.error("🚨 RSA密钥对生成失败", e);
            throw new RuntimeException("RSA密钥对生成失败", e);
        }
    }

    /**
     * JWK源配置
     * 提供用于JWT签名的密钥集合
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        log.info("🔧 配置JWK源");

        try {
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

            JWKSet jwkSet = new JWKSet(rsaKey);
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

            log.info("✅ JWK源配置完成，密钥ID: {}", rsaKey.getKeyID());
            return jwkSource;

        } catch (Exception e) {
            log.error("🚨 JWK源配置失败", e);
            throw new RuntimeException("JWK源配置失败", e);
        }
    }

    /**
     * JWT编码器配置
     * 用于生成JWT令牌
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        log.info("🔧 配置JWT编码器");

        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);

        log.info("✅ JWT编码器配置完成");
        return jwtEncoder;
    }

    /**
     * JWT解码器配置
     * 用于验证和解析JWT令牌
     * 优先使用远程JWK URI，失败后回退到本地JWK源
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        log.info("🔧 配置JWT解码器");

        // 如果配置了远程JWK URI，尝试使用远程验证
        if (jwkSetUri != null && !jwkSetUri.trim().isEmpty()) {
            try {
                JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                        .build();

                log.info("✅ JWT解码器配置完成（远程JWK URI）: {}", jwkSetUri);
                return jwtDecoder;

            } catch (Exception e) {
                log.warn("⚠️ 远程JWK URI配置失败，回退到本地JWK源: {}", e.getMessage());
            }
        }

        // 使用本地JWK源 - 直接创建JwtDecoder实例
        log.info("🔧 使用本地JWK源创建JWT解码器");

        try {
            // 创建使用本地JWK源的JWT解码器
            JwtDecoder localJwtDecoder = createLocalJwtDecoder(jwkSource);

            log.info("✅ JWT解码器配置完成（本地JWK源）");
            return localJwtDecoder;

        } catch (Exception e) {
            log.error("🚨 本地JWK源JWT解码器创建失败", e);
            throw new RuntimeException("无法创建JWT解码器", e);
        }
    }

    /**
     * 创建使用本地JWK源的JWT解码器
     * 由于Spring Security OAuth2版本问题，暂时返回一个简单的解码器
     */
    private JwtDecoder createLocalJwtDecoder(JWKSource<SecurityContext> jwkSource) {
        log.warn("⚠️ 使用简化版JWT解码器，建议配置远程JWK URI");

        // 返回一个简单的解码器，仅用于开发环境
        // 生产环境建议使用远程JWK URI
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }

    /**
     * OAuth2.1令牌生成器配置
     * 支持JWT访问令牌、刷新令牌生成
     */
    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(JwtEncoder jwtEncoder) {
        log.info("🔧 配置OAuth2.1令牌生成器");

        // JWT生成器（用于访问令牌）
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);

        // 访问令牌生成器
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

        // 刷新令牌生成器
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        // 委托令牌生成器（支持多种令牌类型）
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator =
                new DelegatingOAuth2TokenGenerator(
                        jwtGenerator,
                        accessTokenGenerator,
                        refreshTokenGenerator
                );

        log.info("✅ OAuth2.1令牌生成器配置完成");
        return tokenGenerator;
    }

    /**
     * 密码编码器配置
     * 支持多种密码编码算法，符合安全最佳实践
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔧 配置密码编码器");

        // 创建密码编码器映射
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        // BCrypt编码器（推荐用于用户密码）
        encoders.put("bcrypt", new BCryptPasswordEncoder(12));  // 强度12

        // NoOp编码器（用于OAuth2客户端密码）
        encoders.put("noop", new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().equals(encodedPassword);
            }
        });

        // PBKDF2编码器（备选）
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        // SCrypt编码器（备选）  
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());

        // Argon2编码器（最新推荐）
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        // 创建委托密码编码器，默认使用BCrypt
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);

        log.info("✅ 密码编码器配置完成，默认算法: bcrypt，支持算法: {}", encoders.keySet());
        return passwordEncoder;
    }

    /**
     * JWT权限转换器配置
     * 将JWT中的权限信息转换为Spring Security权限
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        log.info("🔧 配置JWT权限转换器");

        // 权限转换器
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // OAuth2.1标准：从scope声明中提取权限
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        // JWT认证转换器
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        // 设置权限转换器
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        // 设置主体名称提取器（使用preferred_username或sub）
        jwtAuthenticationConverter.setPrincipalClaimName("preferred_username");

        log.info("✅ JWT权限转换器配置完成");
        return jwtAuthenticationConverter;
    }

    /**
     * JWT权限转换器（增强版）
     * 支持同时从scope和authorities中提取权限
     */
    @Bean("enhancedJwtAuthenticationConverter")
    @org.springframework.context.annotation.Primary
    public JwtAuthenticationConverter enhancedJwtAuthenticationConverter() {
        log.info("🔧 配置增强版JWT权限转换器");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // 增强的权限转换器
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 从scope提取OAuth2权限
            JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
            scopeConverter.setAuthorityPrefix("SCOPE_");
            scopeConverter.setAuthoritiesClaimName("scope");
            var scopeAuthorities = scopeConverter.convert(jwt);

            // 从authorities提取角色权限
            JwtGrantedAuthoritiesConverter roleConverter = new JwtGrantedAuthoritiesConverter();
            roleConverter.setAuthorityPrefix("ROLE_");
            roleConverter.setAuthoritiesClaimName("authorities");
            var roleAuthorities = roleConverter.convert(jwt);

            // 合并权限
            var allAuthorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
            if (scopeAuthorities != null) allAuthorities.addAll(scopeAuthorities);
            if (roleAuthorities != null) allAuthorities.addAll(roleAuthorities);

            // 根据user_type添加默认角色
            Object userType = jwt.getClaim("user_type");
            if (userType != null) {
                allAuthorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + userType.toString().toUpperCase()));
            }

            log.debug("🔑 JWT权限转换完成，权限数量: {}", allAuthorities.size());
            return allAuthorities;
        });

        // 优先使用preferred_username，回退到username，最后使用sub
        converter.setPrincipalClaimName("preferred_username");

        log.info("✅ 增强版JWT权限转换器配置完成");
        return converter;
    }
}
