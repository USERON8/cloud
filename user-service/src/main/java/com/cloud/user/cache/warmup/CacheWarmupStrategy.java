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

/**
 * 用户缓存预热策略
 * <p>
 * 预热内容:
 * 1. 最近活跃的用户数据 (按更新时间排序,取前100条)
 * 2. 状态正常的用户 (status = 1)
 * <p>
 * 预热目标缓存:
 * - userInfo: 用户基本信息缓存
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final UserMapper userMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {
            log.info("开始执行用户缓存预热策略");

            // 1. 获取缓存实例
            Cache userInfoCache = cacheManager.getCache("userInfo");
            if (userInfoCache == null) {
                log.warn("未找到 userInfo 缓存实例,跳过预热");
                return 0;
            }

            // 2. 查询最近活跃的用户 (按更新时间倒序,取前100条)
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getStatus, 1)  // 只预热启用状态的用户
                    .orderByDesc(User::getUpdatedAt)  // 按更新时间倒序
                    .last("LIMIT 100");  // 限制100条

            List<User> activeUsers = userMapper.selectList(queryWrapper);
            log.info("查询到 {} 个活跃用户,开始预热...", activeUsers.size());

            // 3. 将用户数据预热到缓存
            for (User user : activeUsers) {
                try {
                    // 移除敏感信息
                    User cachedUser = sanitizeUser(user);

                    // 使用用户ID作为缓存key
                    userInfoCache.put(user.getId(), cachedUser);
                    warmedUpCount++;
                } catch (Exception e) {
                    log.warn("预热用户 {} 失败: {}", user.getId(), e.getMessage());
                }
            }

            log.info("用户缓存预热完成: 成功预热 {} 个用户", warmedUpCount);
            return warmedUpCount;

        } catch (Exception e) {
            log.error("用户缓存预热失败", e);
            return warmedUpCount;
        }
    }

    /**
     * 清理用户敏感信息
     * 缓存中不应该包含密码等敏感数据
     */
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
        // 注意: 不复制 password 字段
        return sanitized;
    }

    @Override
    public String getStrategyName() {
        return "UserCacheWarmupStrategy";
    }
}
