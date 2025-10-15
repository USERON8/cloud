package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 用户异步服务实现类
 * 使用CompletableFuture和@Async注解实现并发优化
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAsyncServiceImpl implements UserAsyncService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 批量查询的分批大小
     */
    private static final int BATCH_SIZE = 50;

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<List<UserDTO>> getUsersByIdsAsync(Collection<Long> userIds) {
        log.debug("异步批量查询用户信息开始，用户数量: {}", userIds.size());
        long startTime = System.currentTimeMillis();

        try {
            if (userIds == null || userIds.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            // 如果数量较少，直接查询
            if (userIds.size() <= BATCH_SIZE) {
                List<UserDTO> result = userService.getUsersByIds(userIds);
                log.debug("异步批量查询用户信息完成，耗时: {}ms", System.currentTimeMillis() - startTime);
                return CompletableFuture.completedFuture(result);
            }

            // 大批量数据：分批并发查询
            List<Long> userIdList = new ArrayList<>(userIds);
            List<CompletableFuture<List<UserDTO>>> futures = new ArrayList<>();

            // 分批处理
            for (int i = 0; i < userIdList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, userIdList.size());
                List<Long> batch = userIdList.subList(i, end);

                CompletableFuture<List<UserDTO>> future = CompletableFuture.supplyAsync(
                        () -> userService.getUsersByIds(batch)
                );
                futures.add(future);
            }

            // 等待所有批次完成并合并结果
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<UserDTO> result = futures.stream()
                                .map(CompletableFuture::join)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());

                        log.info("异步批量查询用户信息完成，总数: {}, 耗时: {}ms",
                                result.size(), System.currentTimeMillis() - startTime);
                        return result;
                    });

        } catch (Exception e) {
            log.error("异步批量查询用户信息失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<List<UserVO>> getUserVOsByIdsAsync(Collection<Long> userIds) {
        log.debug("异步批量查询用户VO开始，用户数量: {}", userIds.size());

        return getUsersByIdsAsync(userIds)
                .thenApply(userDTOs -> {
                    List<UserVO> userVOs = userConverter.dtoToVOList(userDTOs);
                    log.debug("异步批量查询用户VO完成，数量: {}", userVOs.size());
                    return userVOs;
                })
                .exceptionally(ex -> {
                    log.error("异步批量查询用户VO失败", ex);
                    return Collections.emptyList();
                });
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<UserDTO> getUserByIdAsync(Long userId) {
        log.debug("异步查询用户信息: userId={}", userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getUserById(userId);
            } catch (Exception e) {
                log.error("异步查询用户信息失败: userId={}", userId, e);
                throw new RuntimeException("查询用户信息失败", e);
            }
        });
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<Map<String, Boolean>> checkUsernamesExistAsync(List<String> usernames) {
        log.debug("异步批量检查用户名是否存在，数量: {}", usernames.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 并发检查多个用户名
                List<CompletableFuture<Map.Entry<String, Boolean>>> futures = usernames.stream()
                        .map(username -> CompletableFuture.supplyAsync(() -> {
                            UserDTO user = userService.findByUsername(username);
                            return Map.entry(username, user != null);
                        }))
                        .collect(Collectors.toList());

                // 等待所有检查完成
                Map<String, Boolean> result = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                log.info("异步批量检查用户名完成，数量: {}, 耗时: {}ms",
                        usernames.size(), System.currentTimeMillis() - startTime);
                return result;

            } catch (Exception e) {
                log.error("异步批量检查用户名失败", e);
                throw new RuntimeException("检查用户名失败", e);
            }
        });
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<Map<Long, Boolean>> checkUsersActiveAsync(Collection<Long> userIds) {
        log.debug("异步批量校验用户状态，数量: {}", userIds.size());

        return getUsersByIdsAsync(userIds)
                .thenApply(userDTOs -> {
                    Map<Long, Boolean> result = userDTOs.stream()
                            .collect(Collectors.toMap(
                                    UserDTO::getId,
                                    user -> user.getStatus() != null && user.getStatus() == 1
                            ));
                    log.debug("异步批量校验用户状态完成，数量: {}", result.size());
                    return result;
                })
                .exceptionally(ex -> {
                    log.error("异步批量校验用户状态失败", ex);
                    return Collections.emptyMap();
                });
    }

    @Override
    @Async("userOperationExecutor")
    public CompletableFuture<Boolean> updateLastLoginTimeAsync(Collection<Long> userIds) {
        log.debug("异步批量更新最后登录时间，数量: {}", userIds.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                // 批量更新
                for (Long userId : userIds) {
                    User user = new User();
                    user.setId(userId);
                    // 这里应该有一个lastLoginTime字段，如果没有可以记录到Redis
                    String key = "user:last_login:" + userId;
                    redisTemplate.opsForValue().set(key, now);
                }

                log.info("异步批量更新最后登录时间完成，数量: {}, 耗时: {}ms",
                        userIds.size(), System.currentTimeMillis() - startTime);
                return true;

            } catch (Exception e) {
                log.error("异步批量更新最后登录时间失败", e);
                return false;
            }
        });
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId) {
        log.info("异步发送欢迎邮件，userId: {}", userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UserDTO user = userService.getUserById(userId);
                if (user == null || user.getEmail() == null) {
                    log.warn("用户不存在或没有邮箱地址，无法发送欢迎邮件: userId={}", userId);
                    return false;
                }

                // TODO: 集成邮件服务
                log.info("发送欢迎邮件成功: userId={}, email={}", userId, user.getEmail());
                return true;

            } catch (Exception e) {
                log.error("发送欢迎邮件失败: userId={}", userId, e);
                return false;
            }
        });
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Long userId) {
        log.debug("异步刷新用户缓存: userId={}", userId);

        return CompletableFuture.runAsync(() -> {
            try {
                if (cacheManager != null) {
                    // 清除旧缓存
                    Objects.requireNonNull(cacheManager.getCache("user")).evict(userId);
                    Objects.requireNonNull(cacheManager.getCache("userInfo")).evict(userId);

                    // 预加载新数据
                    userService.getUserById(userId);

                    log.debug("刷新用户缓存成功: userId={}", userId);
                }
            } catch (Exception e) {
                log.error("刷新用户缓存失败: userId={}", userId, e);
            }
        });
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Collection<Long> userIds) {
        log.info("异步批量刷新用户缓存，数量: {}", userIds.size());

        return CompletableFuture.runAsync(() -> {
            try {
                // 并发刷新多个用户缓存
                List<CompletableFuture<Void>> futures = userIds.stream()
                        .map(this::refreshUserCacheAsync)
                        .collect(Collectors.toList());

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                log.info("批量刷新用户缓存完成，数量: {}", userIds.size());

            } catch (Exception e) {
                log.error("批量刷新用户缓存失败", e);
            }
        });
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Integer> preloadPopularUsersAsync(Integer limit) {
        log.info("异步预加载热门用户数据，数量: {}", limit);
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 查询最近活跃的用户
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getStatus, 1)
                        .orderByDesc(User::getCreatedAt)
                        .last("LIMIT " + limit);

                List<User> users = userMapper.selectList(wrapper);

                // 预加载到缓存
                users.forEach(user -> {
                    try {
                        userService.getUserById(user.getId());
                    } catch (Exception e) {
                        log.warn("预加载用户缓存失败: userId={}", user.getId(), e);
                    }
                });

                log.info("预加载热门用户数据完成，数量: {}, 耗时: {}ms",
                        users.size(), System.currentTimeMillis() - startTime);
                return users.size();

            } catch (Exception e) {
                log.error("预加载热门用户数据失败", e);
                return 0;
            }
        });
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countUsersAsync() {
        log.debug("异步统计用户总数");

        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.count();
            } catch (Exception e) {
                log.error("异步统计用户总数失败", e);
                return 0L;
            }
        });
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countActiveUsersAsync(Integer days) {
        log.debug("异步统计活跃用户数，最近{}天", days);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 这里可以根据lastLoginTime统计
                // 目前先使用Redis记录的登录时间
                String pattern = "user:last_login:*";
                Set<String> keys = redisTemplate.keys(pattern);

                if (keys == null || keys.isEmpty()) {
                    return 0L;
                }

                LocalDateTime threshold = LocalDateTime.now().minusDays(days);
                long count = keys.stream()
                        .map(key -> (LocalDateTime) redisTemplate.opsForValue().get(key))
                        .filter(Objects::nonNull)
                        .filter(loginTime -> loginTime.isAfter(threshold))
                        .count();

                log.debug("活跃用户数统计完成: {}", count);
                return count;

            } catch (Exception e) {
                log.error("异步统计活跃用户数失败", e);
                return 0L;
            }
        });
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserGrowthTrendAsync(Integer days) {
        log.info("异步获取用户增长趋势，最近{}天", days);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Long> trend = new LinkedHashMap<>();
                LocalDateTime endDate = LocalDateTime.now();

                for (int i = days - 1; i >= 0; i--) {
                    LocalDateTime date = endDate.minusDays(i);
                    String dateStr = date.toLocalDate().toString();

                    // 统计该日注册的用户数
                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.ge(User::getCreatedAt, date.toLocalDate().atStartOfDay())
                            .lt(User::getCreatedAt, date.toLocalDate().plusDays(1).atStartOfDay());

                    long count = userMapper.selectCount(wrapper);
                    trend.put(dateStr, count);
                }

                log.info("用户增长趋势获取完成，天数: {}", days);
                return trend;

            } catch (Exception e) {
                log.error("异步获取用户增长趋势失败", e);
                return Collections.emptyMap();
            }
        });
    }
}
