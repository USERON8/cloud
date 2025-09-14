package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 Token Redis存储配置
 * 提供Token存储、监控、统计等功能
 *
 * @author what's up
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "oauth2.token.storage.redis.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2TokenRedisConfig {

    /**
     * OAuth2 Token存储管理器
     * 提供Token的存储、查询、删除、统计等功能
     */
    @Bean
    public OAuth2TokenStorageManager oAuth2TokenStorageManager(RedisTemplate<String, Object> redisTemplate) {
        return new OAuth2TokenStorageManager(redisTemplate);
    }

    /**
     * Redis消息监听容器，用于监听Token过期事件
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisTemplate<String, Object> redisTemplate,
            OAuth2TokenStorageManager tokenStorageManager) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());

        // 监听Token过期事件
        MessageListenerAdapter tokenExpirationListener = new MessageListenerAdapter(
                new TokenExpirationListener(tokenStorageManager), "handleMessage");

        container.addMessageListener(tokenExpirationListener,
                new ChannelTopic("__keyevent@*__:expired"));

        return container;
    }

    /**
     * OAuth2 Token存储管理器实现
     */
    public static class OAuth2TokenStorageManager {

        // Redis key前缀
        private static final String TOKEN_PREFIX = "oauth2:token:";
        private static final String AUTH_PREFIX = "oauth2:authorization:";
        private static final String USER_TOKENS_PREFIX = "oauth2:user_tokens:";
        private static final String CLIENT_TOKENS_PREFIX = "oauth2:client_tokens:";
        private static final String TOKEN_STATS_PREFIX = "oauth2:stats:";
        private final RedisTemplate<String, Object> redisTemplate;
        private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

        public OAuth2TokenStorageManager(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * 存储Token映射到Authorization ID
         *
         * @param tokenValue      Token值
         * @param authorizationId Authorization ID
         * @param expireSeconds   过期秒数
         */
        public void storeTokenMapping(String tokenValue, String authorizationId, long expireSeconds) {
            String tokenKey = TOKEN_PREFIX + tokenValue;
            redisTemplate.opsForValue().set(tokenKey, authorizationId, expireSeconds, java.util.concurrent.TimeUnit.SECONDS);
            activeTokens.add(tokenValue);

            log.debug("Stored token mapping: {} -> {}, expires in {} seconds",
                    tokenValue.substring(0, Math.min(tokenValue.length(), 10)) + "...",
                    authorizationId, expireSeconds);
        }

        /**
         * 根据Token值获取Authorization ID
         *
         * @param tokenValue Token值
         * @return Authorization ID，如果不存在返回null
         */
        public String getAuthorizationIdByToken(String tokenValue) {
            String tokenKey = TOKEN_PREFIX + tokenValue;
            Object result = redisTemplate.opsForValue().get(tokenKey);
            return result != null ? result.toString() : null;
        }

        /**
         * 删除Token映射
         *
         * @param tokenValue Token值
         */
        public void removeTokenMapping(String tokenValue) {
            String tokenKey = TOKEN_PREFIX + tokenValue;
            redisTemplate.delete(tokenKey);
            activeTokens.remove(tokenValue);

            log.debug("Removed token mapping: {}",
                    tokenValue.substring(0, Math.min(tokenValue.length(), 10)) + "...");
        }

        /**
         * 存储用户的Token列表（用于统计和管理）
         *
         * @param userId        用户ID
         * @param tokenValue    Token值
         * @param expireSeconds 过期秒数
         */
        public void addUserToken(String userId, String tokenValue, long expireSeconds) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, tokenValue);
            redisTemplate.expire(userTokensKey, expireSeconds, java.util.concurrent.TimeUnit.SECONDS);
        }

        /**
         * 移除用户的Token
         *
         * @param userId     用户ID
         * @param tokenValue Token值
         */
        public void removeUserToken(String userId, String tokenValue) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, tokenValue);
        }

        /**
         * 获取用户的所有活跃Token
         *
         * @param userId 用户ID
         * @return Token值集合
         */
        public Set<Object> getUserTokens(String userId) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            return redisTemplate.opsForSet().members(userTokensKey);
        }

        /**
         * 撤销用户的所有Token
         *
         * @param userId 用户ID
         */
        public void revokeAllUserTokens(String userId) {
            Set<Object> userTokens = getUserTokens(userId);
            if (userTokens != null && !userTokens.isEmpty()) {
                for (Object token : userTokens) {
                    removeTokenMapping(token.toString());
                }
                String userTokensKey = USER_TOKENS_PREFIX + userId;
                redisTemplate.delete(userTokensKey);

                log.info("Revoked all tokens for user: {}, count: {}", userId, userTokens.size());
            }
        }

        /**
         * 存储客户端的Token列表
         *
         * @param clientId      客户端ID
         * @param tokenValue    Token值
         * @param expireSeconds 过期秒数
         */
        public void addClientToken(String clientId, String tokenValue, long expireSeconds) {
            String clientTokensKey = CLIENT_TOKENS_PREFIX + clientId;
            redisTemplate.opsForSet().add(clientTokensKey, tokenValue);
            redisTemplate.expire(clientTokensKey, expireSeconds, java.util.concurrent.TimeUnit.SECONDS);
        }

        /**
         * 移除客户端的Token
         *
         * @param clientId   客户端ID
         * @param tokenValue Token值
         */
        public void removeClientToken(String clientId, String tokenValue) {
            String clientTokensKey = CLIENT_TOKENS_PREFIX + clientId;
            redisTemplate.opsForSet().remove(clientTokensKey, tokenValue);
        }

        /**
         * 获取客户端的所有活跃Token
         *
         * @param clientId 客户端ID
         * @return Token值集合
         */
        public Set<Object> getClientTokens(String clientId) {
            String clientTokensKey = CLIENT_TOKENS_PREFIX + clientId;
            return redisTemplate.opsForSet().members(clientTokensKey);
        }

        /**
         * 更新Token统计信息
         *
         * @param action 操作类型 (create, revoke, expire)
         */
        public void updateTokenStats(String action) {
            String statsKey = TOKEN_STATS_PREFIX + action + ":" +
                    java.time.LocalDate.now().toString();
            redisTemplate.opsForValue().increment(statsKey);
            // 统计数据保留30天
            redisTemplate.expire(statsKey, 30, java.util.concurrent.TimeUnit.DAYS);
        }

        /**
         * 获取Token统计信息
         *
         * @param action 操作类型
         * @param date   日期
         * @return 统计数量
         */
        public Long getTokenStats(String action, java.time.LocalDate date) {
            String statsKey = TOKEN_STATS_PREFIX + action + ":" + date.toString();
            Object count = redisTemplate.opsForValue().get(statsKey);
            return count != null ? Long.parseLong(count.toString()) : 0L;
        }

        /**
         * 获取当前活跃Token数量
         *
         * @return 活跃Token数量
         */
        public int getActiveTokenCount() {
            return activeTokens.size();
        }

        /**
         * 清理过期的Token统计
         */
        public void cleanupExpiredTokens() {
            // 这个方法可以定期调用来清理内存中的过期Token引用
            // 实际的Redis数据会自动过期
            int sizeBefore = activeTokens.size();

            // 验证Token是否还存在于Redis中
            activeTokens.removeIf(token -> {
                String tokenKey = TOKEN_PREFIX + token;
                return !Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
            });

            int sizeAfter = activeTokens.size();
            if (sizeBefore != sizeAfter) {
                log.info("Cleaned up {} expired token references", sizeBefore - sizeAfter);
            }
        }
    }

    /**
     * Token过期事件监听器
     */
    public static class TokenExpirationListener {

        private final OAuth2TokenStorageManager tokenStorageManager;

        public TokenExpirationListener(OAuth2TokenStorageManager tokenStorageManager) {
            this.tokenStorageManager = tokenStorageManager;
        }

        /**
         * 处理Token过期事件
         *
         * @param message 过期的Key
         */
        public void handleMessage(String message) {
            try {
                if (message.startsWith("oauth2:token:")) {
                    String tokenValue = message.substring("oauth2:token:".length());
                    tokenStorageManager.activeTokens.remove(tokenValue);
                    tokenStorageManager.updateTokenStats("expire");

                    log.debug("Token expired: {}",
                            tokenValue.substring(0, Math.min(tokenValue.length(), 10)) + "...");
                }
            } catch (Exception e) {
                log.error("Error handling token expiration event", e);
            }
        }
    }
}
