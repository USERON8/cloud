package com.cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenService {

    private static final String TOKEN_PREFIX = "token:";
    private static final long TOKEN_EXPIRE_TIME = 3600; // 1小时
    private final StringRedisTemplate stringRedisTemplate;

    public TokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将token和用户ID存储到Redis中
     *
     * @param token  JWT token
     * @param userId 用户ID
     */
    public void storeToken(String token, Long userId) {
        try {
            String key = TOKEN_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
            log.debug("Token存储成功, token: {}, userId: {}", token, userId);
        } catch (Exception e) {
            log.error("Token存储失败, token: {}, userId: {}", token, userId, e);
            throw new RuntimeException("Token存储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证token是否存在且有效
     *
     * @param token JWT token
     * @return 用户ID，如果token无效则返回null
     */
    public Long validateToken(String token) {
        try {
            String key = TOKEN_PREFIX + token;
            String userId = stringRedisTemplate.opsForValue().get(key);
            if (userId != null) {
                // 延长token有效期
                stringRedisTemplate.expire(key, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
                log.debug("Token验证成功, token: {}", token);
                return Long.valueOf(userId);
            }
            log.debug("Token验证失败，token不存在或已过期, token: {}", token);
            return null;
        } catch (Exception e) {
            log.error("Token验证失败, token: {}", token, e);
            throw new RuntimeException("Token验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除token
     *
     * @param token JWT token
     */
    public void removeToken(String token) {
        try {
            String key = TOKEN_PREFIX + token;
            Boolean deleted = stringRedisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.debug("Token删除成功, token: {}", token);
            } else {
                log.debug("Token删除失败，token不存在, token: {}", token);
            }
        } catch (Exception e) {
            log.error("Token删除失败, token: {}", token, e);
            throw new RuntimeException("Token删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新token有效期
     *
     * @param token JWT token
     * @return 是否刷新成功
     */
    public boolean refreshToken(String token) {
        try {
            String key = TOKEN_PREFIX + token;
            String userId = stringRedisTemplate.opsForValue().get(key);
            if (userId != null) {
                // 延长token有效期
                Boolean result = stringRedisTemplate.expire(key, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
                if (result != null && result) {
                    log.debug("Token刷新成功, token: {}", token);
                    return true;
                }
            }
            log.debug("Token刷新失败，token不存在或已过期, token: {}", token);
            return false;
        } catch (Exception e) {
            log.error("Token刷新失败, token: {}", token, e);
            return false;
        }
    }

    /**
     * 获取token剩余过期时间
     *
     * @param token JWT token
     * @return 剩余过期时间（秒），如果token无效则返回-1
     */
    public long getExpireTime(String token) {
        try {
            String key = TOKEN_PREFIX + token;
            Long expireTime = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (expireTime != null && expireTime >= 0) {
                log.debug("Token剩余过期时间: {}秒, token: {}", expireTime, token);
                return expireTime;
            }
            log.debug("Token不存在或已过期, token: {}", token);
            return -1;
        } catch (Exception e) {
            log.error("获取Token过期时间失败, token: {}", token, e);
            return -1;
        }
    }
}