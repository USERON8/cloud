package com.cloud.auth.config;

import com.cloud.auth.service.RedisOAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;

/**
 * OAuth2 Token存储配置类
 * 配置将OAuth2 Token存储到Redis中
 */
@Configuration
public class OAuth2TokenConfig {

    /**
     * 配置OAuth2授权服务，使用Redis存储Token
     *
     * @param redisConnectionFactory      Redis连接工厂
     * @param registeredClientRepository  客户端仓库
     * @param authorizationServerSettings 授权服务器设置
     * @return OAuth2AuthorizationService OAuth2授权服务
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            RedisConnectionFactory redisConnectionFactory,
            RegisteredClientRepository registeredClientRepository,
            AuthorizationServerSettings authorizationServerSettings) {

        // 创建RedisTemplate用于OAuth2Authorization对象的序列化
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 配置Key序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用JDK序列化器作为Value序列化器（处理复杂的OAuth2Authorization对象）
        redisTemplate.afterPropertiesSet();

        // 创建Redis OAuth2授权服务
        return new RedisOAuth2AuthorizationService(redisTemplate, registeredClientRepository, authorizationServerSettings);
    }

    /**
     * 配置OAuth2令牌生成器
     *
     * @param jwtEncoder JWT编码器
     * @return OAuth2TokenGenerator OAuth2令牌生成器
     */
    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> oAuth2TokenGenerator(JwtEncoder jwtEncoder) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }
}