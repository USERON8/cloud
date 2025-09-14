package com.cloud.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth2客户端凭证服务
 * 用于获取服务间调用的访问令牌
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ClientCredentialsService {

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager;
    // 用于防止并发情况下重复获取令牌
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();
    // 用于防止循环调用的锁
    private final ThreadLocal<Boolean> gettingToken = new ThreadLocal<>();
    private final ReentrantLock lock = new ReentrantLock();
    @Value("${spring.security.oauth2.client.registration.client-service.client-id:client-service}")
    private String clientId;

    /**
     * 获取内部API访问令牌
     *
     * @return JWT访问令牌
     */
    public String getInternalApiToken() {
        // 检查是否正在获取令牌，防止循环调用
        if (Boolean.TRUE.equals(gettingToken.get())) {
            log.debug("检测到循环调用，跳过获取内部API令牌");
            return null;
        }

        try {
            lock.lock();
            gettingToken.set(true);

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("client-service")
                    .principal(clientId)
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientServiceManager.authorize(authorizeRequest);

            if (authorizedClient != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                log.debug("成功获取内部API访问令牌");
                return accessToken.getTokenValue();
            } else {
                log.error("无法获取OAuth2授权客户端");
                return null;
            }
        } catch (Exception e) {
            log.error("获取内部API访问令牌失败", e);
            return null;
        } finally {
            gettingToken.remove();
            lock.unlock();
        }
    }
}