package com.cloud.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2授权服务器配置
 * 实现授权码模式和客户端凭证模式
 *
 * @author what's up
 */
@Configuration
@RequiredArgsConstructor
public class AuthServerConfig {

    /**
     * OAuth2授权服务器安全配置 - 纯API模式
     * 只处理OAuth2授权服务器的核心功能
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

        // 启用OpenID Connect支持
        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        http
                .securityMatcher("/oauth2/**", "/.well-known/**")
                .with(authorizationServerConfigurer, Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                // 为授权端点提供基本认证支持
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/**", "/oauth2/token").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()) // 启用HTTP Basic认证用于授权端点
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * 默认安全配置 - 纯API模式，无Web界面
     * 鉴权由网关统一处理，这里允许所有请求
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll() // 允许所有请求，鉴权由网关处理
                )
                .httpBasic(AbstractHttpConfigurer::disable) // 禁用HTTP Basic认证
                .formLogin(AbstractHttpConfigurer::disable) // 禁用表单登录
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ); // 无状态模式

        return http.build();
    }

    /**
     * 配置已注册客户端仓库
     * 存储OAuth2客户端信息
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // OAuth2.1 Web客户端（授权码模式 + PKCE）
        RegisteredClient webClient = RegisteredClient.withId("web-client-id")
                .clientId("web-client")
                .clientSecret("{noop}WebClient@2024#Secure")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:80/authorized") // 通过网关访问
                .redirectUri("http://localhost:3000/callback") // 前端应用回调
                .postLogoutRedirectUri("http://127.0.0.1:80/logged-out")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("read")
                .scope("write")
                .scope("user.read")
                .scope("user.write")
                .scope("order.read")
                .scope("order.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2)) // OAuth2.1推荐短期
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false) // OAuth2.1推荐不重用refresh token
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false) // 信任的Web客户端
                        .requireProofKey(true) // OAuth2.1必须使用PKCE
                        .build())
                .build();

        // OAuth2.1 移动客户端（授权码模式 + PKCE）
        RegisteredClient mobileClient = RegisteredClient.withId("mobile-client-id")
                .clientId("mobile-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // 公共客户端不使用密码
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("app://callback")
                .redirectUri("http://127.0.0.1:80/mobile/callback")
                .scope("read")
                .scope("write")
                .scope("user.read")
                .scope("user.write")
                .scope("order.read")
                .scope("order.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1)) // 移动客户端短期 token
                        .refreshTokenTimeToLive(Duration.ofDays(7)) // 7天刷新
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true) // 移动客户端必须使用PKCE
                        .build())
                .build();

        // 服务间通信客户端（客户端凭证模式）
        RegisteredClient clientServiceClient = RegisteredClient.withId("client-service-id")
                .clientId("client-service")
                .clientSecret("{noop}ClientService@2024#Secure")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("internal_api")
                .scope("service.read")
                .scope("service.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(12)) // 服务间token可以较长
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient, mobileClient, clientServiceClient);
    }

    /**
     * 配置JWT解码器
     * 用于验证JWT Token
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(this.generateRsaKey().toRSAPublicKey()).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create JWT decoder", ex);
        }
    }

    /**
     * 配置JWT编码器
     * 用于生成JWT Token
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 配置JWK源
     * 提供用于JWT签名和验证的密钥
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 生成RSA密钥对
     * 用于JWT Token的签名和验证
     */
    private RSAKey generateRsaKey() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    /**
     * 生成RSA密钥对
     */
    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }


    /**
     * 配置OAuth2.1授权服务器设置
     * 所有端点都通过网关统一访问
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://127.0.0.1:80") // 网关统一入口
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .jwkSetEndpoint("/.well-known/jwks.json")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }

    /**
     * 配置OAuth2授权同意服务
     * 用于存储用户的授权同意信息
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    /**
     * 配置GitHub OAuth2客户端注册
     */
    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
                .clientId("Ov23li4lW4aaO4mlFGRf")  // 从application.yml中的配置
                .clientSecret("6afee51f8c5b77a7b3a20dc6b8e41d9b4c60e55d")  // 从application.yml中的配置
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .scope("read:user", "user:email")
                .build();
    }

    // ClientRegistrationRepository已在OAuth2ClientConfig中定义，避免重复

    /**
     * 配置OAuth2授权客户端仓库
     * 用于存储OAuth2授权客户端信息
     */
    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    // OAuth2AuthorizedClientService已在OAuth2ClientConfig中定义，避免重复
}