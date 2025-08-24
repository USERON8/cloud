package com.cloud.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * 基础JWT配置类，用于资源服务器的JWT解码配置
 * <p>
 * 该配置类提供统一的JWT解码器配置，供各个资源服务使用
 * 通过配置spring.security.oauth2.resourceserver.jwt.jwk-set-uri属性来指定JWK端点
 */
@Configuration
public class BaseJwtConfig {

    /**
     * JWT解码器Bean
     * 当配置了spring.security.oauth2.resourceserver.jwt.jwk-set-uri属性时，
     * 该Bean会自动创建并配置JWT解码器
     *
     * @return JwtDecoder JWT解码器实例
     */
    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    public JwtDecoder jwtDecoder() {
        // 创建JWT解码器，使用Nimbus库实现

        return NimbusJwtDecoder.withJwkSetUri(
                        "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
                .build();
    }
}