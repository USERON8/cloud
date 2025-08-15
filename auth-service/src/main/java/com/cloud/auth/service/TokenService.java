package com.cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenService {

    /**
     * OAuth2授权服务器会自动管理令牌，这里仅作为兼容性保留
     */
    public void storeToken(String token, Long userId) {
        // OAuth2授权服务器会自动管理令牌，无需手动存储
        log.debug("OAuth2模式下无需手动存储token, token: {}, userId: {}", token, userId);
    }

    /**
     * OAuth2授权服务器会自动验证令牌，这里仅作为兼容性保留
     */
    public Long validateToken(String token) {
        // OAuth2授权服务器会自动验证令牌，这里直接返回null表示由OAuth2处理
        log.debug("OAuth2模式下由授权服务器验证token: {}", token);
        return null;
    }

    /**
     * OAuth2授权服务器会自动管理令牌，这里仅作为兼容性保留
     */
    public void removeToken(String token) {
        // OAuth2授权服务器会自动管理令牌，无需手动删除
        log.debug("OAuth2模式下无需手动删除token: {}", token);
    }

    /**
     * OAuth2授权服务器会自动管理令牌，这里仅作为兼容性保留
     */
    public boolean refreshToken(String token) {
        // OAuth2授权服务器会自动管理令牌，这里直接返回false表示由OAuth2处理
        log.debug("OAuth2模式下由授权服务器处理token刷新: {}", token);
        return false;
    }

    /**
     * OAuth2授权服务器会自动管理令牌过期时间，这里仅作为兼容性保留
     */
    public long getExpireTime(String token) {
        // OAuth2授权服务器会自动管理令牌过期时间，这里直接返回-1表示由OAuth2处理
        log.debug("OAuth2模式下由授权服务器管理token过期时间: {}", token);
        return -1;
    }
}