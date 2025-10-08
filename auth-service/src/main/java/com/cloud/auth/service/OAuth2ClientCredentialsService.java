package com.cloud.auth.service;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.lock.RedissonLockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * OAuth2客户端凭证服务
 * 用于获取服务间调用的访问令牌
 *
 * @author what's up
 */
@Slf4j
@Service
public class OAuth2ClientCredentialsService {

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager;
    // 用于防止循环调用的标记
    private final ThreadLocal<Boolean> gettingToken = new ThreadLocal<>();
    @Autowired(required = false)
    private RedissonLockManager redissonLockManager;
    @Value("${spring.security.oauth2.client.registration.client-service.client-id:client-service}")
    private String clientId;

    public OAuth2ClientCredentialsService(AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceManager) {
        this.authorizedClientServiceManager = authorizedClientServiceManager;
    }

    /**
     * 获取内部API访问令牌
     * 使用分布式锁防止并发获取令牌
     *
     * @return JWT访问令牌
     */
    @DistributedLock(
            key = "'oauth2:token:' + #clientId",
            waitTime = 3,
            leaseTime = 10,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL,
            failMessage = "获取OAuth2令牌锁失败"
    )
    public String getInternalApiToken() {
        // 检查是否正在获取令牌，防止循环调用
        if (Boolean.TRUE.equals(gettingToken.get())) {
            log.debug("检测到循环调用，跳过获取内部API令牌");
            return null;
        }

        try {
            gettingToken.set(true);

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("client-service")
                    .principal(clientId)
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientServiceManager.authorize(authorizeRequest);

            if (authorizedClient != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                log.debug("✅ 成功获取内部API访问令牌");
                return accessToken.getTokenValue();
            } else {
                log.error("❌ 无法获取OAuth2授权客户端");
                return null;
            }
        } catch (Exception e) {
            log.error("❌ 获取内部API访问令牌失败", e);
            return null;
        } finally {
            gettingToken.remove();
        }
    }

    /**
     * 编程式获取令牌（备用方法）
     * 使用RedissonLockManager进行更细粒度的控制
     *
     * @return JWT访问令牌
     */
    public String getInternalApiTokenProgrammatic() {
        // 检查是否正在获取令牌，防止循环调用
        if (Boolean.TRUE.equals(gettingToken.get())) {
            log.debug("检测到循环调用，跳过获取内部API令牌");
            return null;
        }

        // 如果没有RedissonLockManager，使用同步方法
        if (redissonLockManager == null) {
            log.warn("⚠️ RedissonLockManager未配置，使用同步方法获取令牌");
            return getTokenWithoutLock();
        }

        String lockKey = "oauth2:token:programmatic:" + clientId;

        return redissonLockManager.executeWithLock(lockKey, 2, 8, TimeUnit.SECONDS, () -> {
            try {
                gettingToken.set(true);

                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId("client-service")
                        .principal(clientId)
                        .build();

                OAuth2AuthorizedClient authorizedClient = authorizedClientServiceManager.authorize(authorizeRequest);

                if (authorizedClient != null) {
                    OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                    log.debug("✅ 编程式获取内部API访问令牌成功");
                    return accessToken.getTokenValue();
                } else {
                    log.error("❌ 编程式获取OAuth2授权客户端失败");
                    return null;
                }
            } catch (Exception e) {
                log.error("❌ 编程式获取内部API访问令牌失败", e);
                return null;
            } finally {
                gettingToken.remove();
            }
        });
    }

    /**
     * 不使用分布式锁的令牌获取方法
     * 当RedissonLockManager不可用时的备用方案
     *
     * @return JWT访问令牌
     */
    private synchronized String getTokenWithoutLock() {
        try {
            gettingToken.set(true);

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("client-service")
                    .principal("client-service")
                    .build();

            var authorizedClient = authorizedClientServiceManager.authorize(authorizeRequest);

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String token = authorizedClient.getAccessToken().getTokenValue();
                log.debug("✅ 成功获取内部API令牌（同步方式）: {}...",
                        token.length() > 20 ? token.substring(0, 20) : token);
                return token;
            } else {
                log.error("❌ 无法获取内部API令牌（同步方式）");
                return null;
            }

        } catch (Exception e) {
            log.error("❌ 获取内部API令牌异常（同步方式）", e);
            return null;
        } finally {
            gettingToken.remove();
        }
    }
}