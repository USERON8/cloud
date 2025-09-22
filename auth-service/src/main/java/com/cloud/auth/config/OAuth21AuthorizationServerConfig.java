package com.cloud.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2.1授权服务器核心配置
 * <p>
 * 此配置类作为OAuth2.1授权服务器的统一入口，整合了以下功能：
 * - 授权服务器设置
 * - 注册客户端管理
 * - 令牌设置（访问令牌、刷新令牌、授权码等）
 * - 客户端设置（PKCE、授权码流程等）
 * - 导入其他相关配置类
 * <p>
 * 严格遵循OAuth2.1规范：
 * - 强制使用PKCE（Proof Key for Code Exchange）
 * - 不重用刷新令牌（Token Rotation）
 * - 支持OpenID Connect（OIDC）
 * - 使用JWT格式的访问令牌
 *
 * @author what's up
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Import({
        SecurityFilterChainConfig.class,    // 安全过滤器链配置
        JwtPasswordConfig.class,           // JWT和密码编码配置
        AuthenticationProviderConfig.class, // 认证提供者配置
        RedisOAuth2Config.class            // Redis OAuth2存储配置
})
public class OAuth21AuthorizationServerConfig {

    /**
     * OAuth2.1授权服务器设置
     * <p>
     * 配置授权服务器的各种端点和设置，符合OAuth2.1标准
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        log.info("🔧 配置OAuth2.1授权服务器设置");

        AuthorizationServerSettings settings = AuthorizationServerSettings.builder()
                // OAuth2.1标准端点配置
                .issuer("http://localhost:8080")                    // 发行者URL
                .authorizationEndpoint("/oauth2/authorize")         // 授权端点
                .tokenEndpoint("/oauth2/token")                     // 令牌端点  
                .tokenIntrospectionEndpoint("/oauth2/introspect")   // 令牌内省端点
                .tokenRevocationEndpoint("/oauth2/revoke")          // 令牌撤销端点
                .jwkSetEndpoint("/oauth2/jwks")                     // JWK集合端点
                .oidcLogoutEndpoint("/connect/logout")              // OIDC登出端点
                .oidcUserInfoEndpoint("/userinfo")                  // OIDC用户信息端点
                .oidcClientRegistrationEndpoint("/connect/register") // OIDC客户端注册端点
                .build();

        log.info("✅ OAuth2.1授权服务器设置配置完成");
        return settings;
    }

    /**
     * 注册客户端仓库配置
     * <p>
     * 管理OAuth2客户端信息，支持多种授权流程
     * 严格遵循OAuth2.1安全要求
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        log.info("🔧 配置OAuth2.1注册客户端仓库");

        // 创建默认的Web应用客户端（支持授权码流程 + PKCE）
        RegisteredClient webAppClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("webapp-client")
                .clientSecret("{noop}webapp-secret")  // 开发环境使用简单密码，生产环境需要加密

                // OAuth2.1推荐的认证方法
                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })

                // OAuth2.1支持的授权类型
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                })

                // 回调URL配置
                .redirectUris(uris -> {
                    uris.add("http://localhost:3000/callback");      // 前端应用回调
                    uris.add("http://localhost:8080/login/callback"); // 后端应用回调
                    uris.add("http://127.0.0.1:8080/login/callback"); // 本地回调
                })

                // OAuth2.1标准作用域 + 自定义作用域
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID);
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add(OidcScopes.EMAIL);
                    scopes.add("read");
                    scopes.add("write");
                    scopes.add("admin");
                })

                // 客户端设置 - 强制使用OAuth2.1安全特性
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)  // 是否需要用户确认授权
                        .requireProofKey(true)               // OAuth2.1要求：强制使用PKCE
                        .build())

                // 令牌设置 - 符合OAuth2.1安全建议
                .tokenSettings(createSecureTokenSettings())

                .build();

        // 创建移动应用客户端（公共客户端，仅支持PKCE）
        RegisteredClient mobileAppClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("mobile-client")
                // 公共客户端不需要密钥

                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.NONE);  // 公共客户端
                })

                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                })

                .redirectUris(uris -> {
                    uris.add("com.example.app://callback");      // 移动应用回调
                })

                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID);
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add("read");
                })

                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)               // 移动应用必须使用PKCE
                        .build())

                .tokenSettings(createMobileTokenSettings())

                .build();

        // 创建服务间通信客户端（客户端凭证流程）
        RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("service-client")
                .clientSecret("{bcrypt}$2a$12$MwWVVHU9XUgKnUC8XKDI3OQPv0WA8Glt1Y6.1X1lVZp7ywdMqF.2S")  // 生产环境密码

                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })

                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);  // 仅客户端凭证流程
                })

                .scopes(scopes -> {
                    scopes.add("service.read");
                    scopes.add("service.write");
                    scopes.add("service.admin");
                })

                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)              // 服务端通信不需要PKCE
                        .build())

                .tokenSettings(createServiceTokenSettings())

                .build();

        // 创建内部服务调用客户端（用于auth-service调用user-service）
        RegisteredClient internalServiceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client-service")
                .clientSecret("{bcrypt}$2a$12$MwWVVHU9XUgKnUC8XKDI3OQPv0WA8Glt1Y6.1X1lVZp7ywdMqF.2S")  // ClientService@2024#Secure

                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })

                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);  // 仅客户端凭证流程
                })

                .scopes(scopes -> {
                    scopes.add("internal_api");  // 内部API访问权限
                })

                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)              // 服务端通信不需要PKCE
                        .build())

                .tokenSettings(createServiceTokenSettings())

                .build();

        RegisteredClientRepository repository = new InMemoryRegisteredClientRepository(
                webAppClient, mobileAppClient, serviceClient, internalServiceClient
        );

        log.info("✅ OAuth2.1注册客户端仓库配置完成，注册客户端数量: 4");
        log.info("   - Web应用客户端: {} (支持授权码+PKCE)", webAppClient.getClientId());
        log.info("   - 移动应用客户端: {} (公共客户端+PKCE)", mobileAppClient.getClientId());
        log.info("   - 服务客户端: {} (客户端凭证)", serviceClient.getClientId());
        log.info("   - 内部服务调用客户端: {} (内部API访问)", internalServiceClient.getClientId());

        return repository;
    }

    /**
     * 创建Web应用的安全令牌设置
     * 符合OAuth2.1安全建议
     */
    private TokenSettings createSecureTokenSettings() {
        return TokenSettings.builder()
                // 访问令牌设置
                .accessTokenTimeToLive(Duration.ofMinutes(30))        // 访问令牌30分钟有效期

                // 刷新令牌设置 - OAuth2.1特性
                .refreshTokenTimeToLive(Duration.ofDays(7))           // 刷新令牌7天有效期
                .reuseRefreshTokens(false)                            // OAuth2.1要求：不重用刷新令牌

                // 授权码设置
                .authorizationCodeTimeToLive(Duration.ofMinutes(10))  // 授权码10分钟有效期

                // ID令牌设置（OpenID Connect）
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)

                .build();
    }

    /**
     * 创建移动应用的令牌设置
     * 移动应用有特殊的安全考虑
     */
    private TokenSettings createMobileTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))        // 移动应用访问令牌15分钟

                .refreshTokenTimeToLive(Duration.ofDays(30))          // 移动应用刷新令牌30天
                .reuseRefreshTokens(false)                            // 不重用刷新令牌

                .authorizationCodeTimeToLive(Duration.ofMinutes(5))   // 移动应用授权码5分钟

                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)

                .build();
    }

    /**
     * 创建服务间通信的令牌设置
     * 服务间通信通常需要更长的令牌有效期
     */
    private TokenSettings createServiceTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))           // 服务令牌2小时有效期

                // 客户端凭证流程不使用刷新令牌，但必须设置为正值
                .refreshTokenTimeToLive(Duration.ofDays(1))           // 设置为1天（虽然不会使用）
                .reuseRefreshTokens(false)

                .build();
    }
}
