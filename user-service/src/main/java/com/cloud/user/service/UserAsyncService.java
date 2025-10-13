package com.cloud.user.service;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用户异步服务接口
 * 提供各种异步操作，优化性能和用户体验
 *
 * @author what's up
 */
public interface UserAsyncService {

    /**
     * 异步批量查询用户信息（并发优化）
     * 将大批量查询拆分为多个小批次并行执行
     *
     * @param userIds 用户ID集合
     * @return 用户DTO列表的Future
     */
    CompletableFuture<List<UserDTO>> getUsersByIdsAsync(Collection<Long> userIds);

    /**
     * 异步批量查询用户VO（带详细信息）
     *
     * @param userIds 用户ID集合
     * @return 用户VO列表的Future
     */
    CompletableFuture<List<UserVO>> getUserVOsByIdsAsync(Collection<Long> userIds);

    /**
     * 异步查询单个用户信息
     *
     * @param userId 用户ID
     * @return 用户DTO的Future
     */
    CompletableFuture<UserDTO> getUserByIdAsync(Long userId);

    /**
     * 异步批量检查用户名是否存在
     *
     * @param usernames 用户名列表
     * @return Map<用户名, 是否存在>
     */
    CompletableFuture<Map<String, Boolean>> checkUsernamesExistAsync(List<String> usernames);

    /**
     * 异步批量校验用户状态
     *
     * @param userIds 用户ID集合
     * @return Map<用户ID, 是否激活>
     */
    CompletableFuture<Map<Long, Boolean>> checkUsersActiveAsync(Collection<Long> userIds);

    /**
     * 异步批量更新用户最后登录时间
     *
     * @param userIds 用户ID集合
     * @return 更新结果Future
     */
    CompletableFuture<Boolean> updateLastLoginTimeAsync(Collection<Long> userIds);

    /**
     * 异步发送用户欢迎邮件
     *
     * @param userId 用户ID
     * @return 发送结果Future
     */
    CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId);

    /**
     * 异步刷新用户缓存
     *
     * @param userId 用户ID
     * @return 刷新结果Future
     */
    CompletableFuture<Void> refreshUserCacheAsync(Long userId);

    /**
     * 异步批量刷新用户缓存
     *
     * @param userIds 用户ID集合
     * @return 刷新结果Future
     */
    CompletableFuture<Void> refreshUserCacheAsync(Collection<Long> userIds);

    /**
     * 异步预加载热门用户数据到缓存
     *
     * @param limit 预加载数量
     * @return 预加载结果Future
     */
    CompletableFuture<Integer> preloadPopularUsersAsync(Integer limit);

    /**
     * 异步统计用户总数
     *
     * @return 用户总数Future
     */
    CompletableFuture<Long> countUsersAsync();

    /**
     * 异步统计活跃用户数
     *
     * @param days 最近天数
     * @return 活跃用户数Future
     */
    CompletableFuture<Long> countActiveUsersAsync(Integer days);

    /**
     * 异步获取用户增长趋势数据
     *
     * @param days 最近天数
     * @return 增长趋势数据Future
     */
    CompletableFuture<Map<String, Long>> getUserGrowthTrendAsync(Integer days);
}
