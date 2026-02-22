package com.cloud.user.cache.warmup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;














@Component
@Slf4j
@RequiredArgsConstructor
public class CacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final UserMapper userMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {
            

            return warmedUpCount;

        } catch (Exception e) {
            log.error("鐢ㄦ埛缂撳瓨棰勭儹澶辫触", e);
            return warmedUpCount;
        }
    }

    



    private User sanitizeUser(User user) {
        User sanitized = new User();
        sanitized.setId(user.getId());
        sanitized.setUsername(user.getUsername());
        sanitized.setNickname(user.getNickname());
        sanitized.setAvatarUrl(user.getAvatarUrl());
        sanitized.setPhone(user.getPhone());
        sanitized.setEmail(user.getEmail());
        sanitized.setStatus(user.getStatus());
        sanitized.setUserType(user.getUserType());
        sanitized.setCreatedAt(user.getCreatedAt());
        sanitized.setUpdatedAt(user.getUpdatedAt());
        
        return sanitized;
    }

    @Override
    public String getStrategyName() {
        return "UserCacheWarmupStrategy";
    }
}
