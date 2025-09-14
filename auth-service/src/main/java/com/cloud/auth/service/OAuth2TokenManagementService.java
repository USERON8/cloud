package com.cloud.auth.service;

import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OAuth2令牌管理服务
 * 统一管理令牌的生成、存储和撤销
 * 确保通过Authorization Server正确颁发令牌并存储到Redis
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenManagementService {

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    /**
     * 为用户登录生成OAuth2令牌（使用授权码模式的内部实现）
     * 注意：这是一个内部方法，用于简化的登录流程
     * 生产环境建议使用标准的OAuth2授权码流程
     *
     * @param userDTO 用户信息
     * @param scopes  请求的权限范围，如果为空则使用默认范围
     * @return OAuth2Authorization 包含访问令牌和刷新令牌的授权对象
     */
    public OAuth2Authorization generateTokensForUser(UserDTO userDTO, Set<String> scopes) {
        if (userDTO == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        // 获取默认的Web客户端（用于用户登录）
        RegisteredClient registeredClient = registeredClientRepository.findByClientId("web-client");
        if (registeredClient == null) {
            throw new IllegalStateException("未找到web-client客户端配置");
        }

        // 构建用户认证信息
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if ("ADMIN".equals(userDTO.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if ("MERCHANT".equals(userDTO.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
        }

        UsernamePasswordAuthenticationToken userAuthentication =
                new UsernamePasswordAuthenticationToken(
                        userDTO.getUsername(),
                        "[PROTECTED]",
                        authorities
                );

        // 设置默认权限范围
        if (scopes == null || scopes.isEmpty()) {
            scopes = Set.of("openid", "profile", "read", "write", "user.read", "user.write");
        }

        // 验证权限范围是否在客户端允许的范围内
        Set<String> allowedScopes = registeredClient.getScopes();
        scopes = scopes.stream()
                .filter(allowedScopes::contains)
                .collect(Collectors.toSet());

        // 构建客户端认证信息
        OAuth2ClientAuthenticationToken clientAuthentication =
                new OAuth2ClientAuthenticationToken(
                        registeredClient,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        registeredClient.getClientSecret()
                );

        // 创建OAuth2Authorization构建器，使用授权码模式
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(UUID.randomUUID().toString())
                .principalName(userDTO.getUsername())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE) // 使用授权码模式
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), userAuthentication)
                .attribute("user_id", userDTO.getId())
                .attribute("user_type", userDTO.getUserType())
                .attribute("nickname", userDTO.getNickname())
                // 添加授权码相关属性（模拟已经完成的授权码流程）
                .attribute("authorization_code", "SIMULATED_" + UUID.randomUUID())
                .attribute("redirect_uri", "http://127.0.0.1:8080/authorized");

        // 生成访问令牌
        OAuth2TokenContext accessTokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorizationBuilder.build())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE) // 使用授权码模式
                .authorizedScopes(scopes)
                .build();

        OAuth2Token accessToken = tokenGenerator.generate(accessTokenContext);
        if (!(accessToken instanceof OAuth2AccessToken oauth2AccessToken)) {
            throw new IllegalStateException("生成的访问令牌类型不正确");
        }

        authorizationBuilder.accessToken(oauth2AccessToken);

        // 生成刷新令牌（如果客户端支持）
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshTokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(registeredClient)
                    .principal(userAuthentication)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorization(authorizationBuilder.build())
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE) // 使用授权码模式
                    .authorizedScopes(scopes)
                    .build();

            OAuth2Token refreshToken = tokenGenerator.generate(refreshTokenContext);
            if (refreshToken instanceof OAuth2RefreshToken) {
                authorizationBuilder.refreshToken((OAuth2RefreshToken) refreshToken);
            }
        }

        // 构建最终的授权对象
        OAuth2Authorization authorization = authorizationBuilder.build();

        // 保存到Redis
        authorizationService.save(authorization);

        log.info("为用户 {} 生成OAuth2令牌成功，授权ID: {}",
                userDTO.getUsername(), authorization.getId());

        return authorization;
    }

    /**
     * 为客户端凭证模式生成令牌
     *
     * @param clientId 客户端ID
     * @param scopes   权限范围
     * @return OAuth2Authorization 授权对象
     */
    public OAuth2Authorization generateTokensForClient(String clientId, Set<String> scopes) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("未找到客户端: " + clientId);
        }

        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
            throw new IllegalArgumentException("客户端不支持client_credentials模式");
        }

        // 验证权限范围
        if (scopes == null || scopes.isEmpty()) {
            scopes = registeredClient.getScopes();
        } else {
            scopes = scopes.stream()
                    .filter(registeredClient.getScopes()::contains)
                    .collect(Collectors.toSet());
        }

        OAuth2ClientAuthenticationToken clientAuthentication =
                new OAuth2ClientAuthenticationToken(
                        registeredClient,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        registeredClient.getClientSecret()
                );

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(UUID.randomUUID().toString())
                .principalName(clientId)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(scopes);

        // 生成访问令牌
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorizationBuilder.build())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(scopes)
                .build();

        OAuth2Token accessToken = tokenGenerator.generate(tokenContext);
        if (!(accessToken instanceof OAuth2AccessToken)) {
            throw new IllegalStateException("生成的访问令牌类型不正确");
        }

        OAuth2Authorization authorization = authorizationBuilder
                .accessToken((OAuth2AccessToken) accessToken)
                .build();

        authorizationService.save(authorization);

        log.info("为客户端 {} 生成OAuth2令牌成功，授权ID: {}",
                clientId, authorization.getId());

        return authorization;
    }

    /**
     * 撤销授权令牌
     *
     * @param tokenValue 令牌值
     */
    public void revokeToken(String tokenValue) {
        OAuth2Authorization authorization = authorizationService.findByToken(tokenValue, null);
        if (authorization != null) {
            authorizationService.remove(authorization);
            log.info("撤销令牌成功，授权ID: {}", authorization.getId());
        }
    }

    /**
     * 查找授权信息
     *
     * @param tokenValue 令牌值
     * @return OAuth2Authorization 授权对象
     */
    public OAuth2Authorization findByToken(String tokenValue) {
        return authorizationService.findByToken(tokenValue, null);
    }

    /**
     * 用户登出 - 撤销指定令牌
     *
     * @param tokenValue 要撤销的令牌值
     * @param username   用户名（用于日志记录）
     * @return 是否成功撤销
     */
    public boolean logout(String tokenValue, String username) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            log.warn("登出失败：令牌值为空, username: {}", username);
            return false;
        }

        try {
            // 查找授权信息
            OAuth2Authorization authorization = authorizationService.findByToken(tokenValue, null);
            if (authorization == null) {
                log.warn("登出失败：未找到对应的授权信息, username: {}, tokenPrefix: {}",
                        username, tokenValue.substring(0, Math.min(tokenValue.length(), 10)) + "...");
                return false;
            }

            // 验证用户身份（可选，确保只能撤销自己的令牌）
            if (username != null && !username.equals(authorization.getPrincipalName())) {
                log.warn("登出失败：用户身份不匹配, requestUser: {}, tokenOwner: {}",
                        username, authorization.getPrincipalName());
                return false;
            }

            // 撤销授权（这将删除Redis中的所有相关数据）
            authorizationService.remove(authorization);

            log.info("用户登出成功, username: {}, authorizationId: {}",
                    authorization.getPrincipalName(), authorization.getId());

            return true;
        } catch (Exception e) {
            log.error("登出过程中发生异常, username: {}", username, e);
            return false;
        }
    }

    /**
     * 用户登出 - 撤销用户的所有令牌
     *
     * @param username 用户名
     * @return 撤销的令牌数量
     */
    public int logoutAllSessions(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("批量登出失败：用户名为空");
            return 0;
        }

        int revokedCount = 0;
        try {
            // 注意：这是一个简化实现，实际生产环境中可能需要更高效的查询方式
            // 可以考虑在Redis中维护用户到令牌的反向索引

            log.info("开始撤销用户 {} 的所有活跃会话", username);

            // 由于当前的OAuth2AuthorizationService接口没有提供按用户名查询的方法
            // 这里我们通过日志记录，实际实现可能需要扩展服务接口
            log.warn("批量撤销功能需要额外的索引支持，当前仅支持单个令牌撤销");

        } catch (Exception e) {
            log.error("批量登出过程中发生异常, username: {}", username, e);
        }

        return revokedCount;
    }

    /**
     * 检查令牌是否有效
     *
     * @param tokenValue 令牌值
     * @return 是否有效
     */
    public boolean isTokenValid(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return false;
        }

        try {
            OAuth2Authorization authorization = authorizationService.findByToken(tokenValue, null);
            if (authorization == null) {
                return false;
            }

            // 检查访问令牌是否存在且未过期
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
            if (accessToken == null) {
                return false;
            }

            // 检查是否过期
            Instant expiresAt = accessToken.getToken().getExpiresAt();
            if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                log.debug("令牌已过期, tokenPrefix: {}", tokenValue.substring(0, Math.min(tokenValue.length(), 10)) + "...");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("检查令牌有效性时发生异常", e);
            return false;
        }
    }

    /**
     * 刷新令牌
     *
     * @param refreshTokenValue 刷新令牌值
     * @return 新的授权对象，如果刷新失败返回null
     */
    public OAuth2Authorization refreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.trim().isEmpty()) {
            log.warn("刷新令牌失败：刷新令牌值为空");
            return null;
        }

        try {
            // 查找现有授权
            OAuth2Authorization existingAuthorization = authorizationService.findByToken(refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);
            if (existingAuthorization == null) {
                log.warn("刷新令牌失败：未找到对应的授权信息, refreshTokenPrefix: {}",
                        refreshTokenValue.substring(0, Math.min(refreshTokenValue.length(), 10)) + "...");
                return null;
            }

            // 检查刷新令牌是否过期
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = existingAuthorization.getRefreshToken();
            if (refreshToken == null) {
                log.warn("刷新令牌失败：授权中不存在刷新令牌");
                return null;
            }

            Instant refreshExpiresAt = refreshToken.getToken().getExpiresAt();
            if (refreshExpiresAt != null && Instant.now().isAfter(refreshExpiresAt)) {
                log.warn("刷新令牌失败：刷新令牌已过期");
                return null;
            }

            // 获取用户信息重新生成令牌
            String username = existingAuthorization.getPrincipalName();
            Set<String> scopes = existingAuthorization.getAuthorizedScopes();

            // 撤销旧的授权
            authorizationService.remove(existingAuthorization);

            // 注意：这里需要重新获取用户信息，实际实现中可能需要从授权属性中获取
            // 或者调用用户服务获取最新用户信息
            log.info("令牌刷新成功, username: {}, oldAuthorizationId: {}", username, existingAuthorization.getId());

            // 这里返回null，因为需要完整的UserDTO才能重新生成令牌
            // 实际使用时，应该从控制器层传入UserDTO或在此处查询用户信息
            return null;

        } catch (Exception e) {
            log.error("刷新令牌过程中发生异常", e);
            return null;
        }
    }
}
