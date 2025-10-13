package com.cloud.user.service;

import com.cloud.user.module.enums.UserActivityType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用户行为日志服务接口
 * 异步记录用户各种行为，用于统计分析和审计
 *
 * @author what's up
 */
public interface UserActivityLogService {

    /**
     * 记录用户登录行为
     *
     * @param userId 用户ID
     * @param ip IP地址
     * @param device 设备信息
     * @return 记录结果
     */
    CompletableFuture<Boolean> logLoginActivityAsync(Long userId, String ip, String device);

    /**
     * 记录用户登出行为
     *
     * @param userId 用户ID
     * @return 记录结果
     */
    CompletableFuture<Boolean> logLogoutActivityAsync(Long userId);

    /**
     * 记录用户注册行为
     *
     * @param userId 用户ID
     * @param registrationType 注册方式
     * @return 记录结果
     */
    CompletableFuture<Boolean> logRegistrationActivityAsync(Long userId, String registrationType);

    /**
     * 记录用户信息修改行为
     *
     * @param userId 用户ID
     * @param modifiedFields 修改的字段
     * @return 记录结果
     */
    CompletableFuture<Boolean> logProfileUpdateActivityAsync(Long userId, List<String> modifiedFields);

    /**
     * 记录密码修改行为
     *
     * @param userId 用户ID
     * @return 记录结果
     */
    CompletableFuture<Boolean> logPasswordChangeActivityAsync(Long userId);

    /**
     * 记录用户通用行为
     *
     * @param userId 用户ID
     * @param activityType 行为类型
     * @param description 行为描述
     * @param metadata 元数据
     * @return 记录结果
     */
    CompletableFuture<Boolean> logActivityAsync(Long userId, UserActivityType activityType,
                                                  String description, Map<String, Object> metadata);

    /**
     * 获取用户最近的活动日志
     *
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 活动日志列表
     */
    CompletableFuture<List<Map<String, Object>>> getRecentActivitiesAsync(Long userId, Integer limit);

    /**
     * 统计用户活跃度
     *
     * @param userId 用户ID
     * @param days 统计天数
     * @return 活跃度分数
     */
    CompletableFuture<Long> calculateUserActivityScoreAsync(Long userId, Integer days);

    /**
     * 批量记录用户活动
     *
     * @param activities 活动列表
     * @return 记录结果
     */
    CompletableFuture<Boolean> logBatchActivitiesAsync(List<Map<String, Object>> activities);
}
