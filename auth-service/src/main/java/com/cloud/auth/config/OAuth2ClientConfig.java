package com.cloud.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OAuth2ClientConfig {

    @Value("${AUTH_TOKEN_URI:http://127.0.0.1:8081/oauth2/token}")
    private String authTokenUri;

    /**
     * 配置 OAuth2 客户端注册仓库
     * 用于客户端凭证模式
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        // GitHub客户端注册
        ClientRegistration githubClient = ClientRegistration.withRegistrationId("github")
                .clientId("Ov23li4lW4aaO4mlFGRf")
                .clientSecret("6afee51f8c5b77a7b3a20dc6b8e41d9b4c60e55d")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();

        registrations.add(githubClient);

        // 客户端服务注册，用于服务间调用
        ClientRegistration clientService = ClientRegistration.withRegistrationId("client-service")
                .clientId("client-service")
                .clientSecret("ClientService@2024#Secure")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(authTokenUri)
                .scope("internal_api")
                .build();

        registrations.add(clientService);

        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * 配置 OAuth2 授权客户端服务
     */
    @Bean
    public OAuth2AuthorizedClientService oauth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    /**
     * 配置服务型 OAuth2 授权客户端管理器
     * 用于客户端凭证模式
     */
    @Bean
    public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    // 移除重复的ClientRegistrationRepository定义，避免与AuthServerConfig冲突
    // AuthServerConfig已经定义了clientRegistrationRepository，这里不再重复定义
    // @Bean
    // public ClientRegistrationRepository oauth2ClientRegistrationRepository() {
    //     ...
    // }
}
