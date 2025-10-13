package com.cloud.user.service;

import java.util.concurrent.CompletableFuture;

/**
 * 用户通知服务接口
 * 提供各种用户通知功能，支持异步发送
 *
 * @author what's up
 */
public interface UserNotificationService {

    /**
     * 发送欢迎邮件
     *
     * @param userId 用户ID
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId);

    /**
     * 发送密码重置邮件
     *
     * @param userId 用户ID
     * @param resetToken 重置令牌
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken);

    /**
     * 发送账户激活邮件
     *
     * @param userId 用户ID
     * @param activationToken 激活令牌
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken);

    /**
     * 发送账户状态变更通知
     *
     * @param userId 用户ID
     * @param newStatus 新状态
     * @param reason 变更原因
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendStatusChangeNotificationAsync(Long userId, Integer newStatus, String reason);

    /**
     * 发送批量通知
     *
     * @param userIds 用户ID列表
     * @param title 通知标题
     * @param content 通知内容
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendBatchNotificationAsync(java.util.List<Long> userIds, String title, String content);

    /**
     * 发送系统公告
     *
     * @param title 公告标题
     * @param content 公告内容
     * @return 发送结果
     */
    CompletableFuture<Boolean> sendSystemAnnouncementAsync(String title, String content);
}
