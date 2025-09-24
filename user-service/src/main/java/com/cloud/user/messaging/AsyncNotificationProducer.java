package com.cloud.user.messaging;

import com.cloud.common.domain.event.NotificationEvent;
import com.cloud.common.messaging.AsyncMessageProducer;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务异步通知生产者
 * 专门用于用户相关的通知消息发送
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncNotificationProducer {

    private static final String NOTIFICATION_BINDING_NAME = "notificationProducer-out-0";
    private final AsyncMessageProducer asyncMessageProducer;

    /**
     * 异步发送用户注册邮件通知
     */
    @Async("userNotificationExecutor")
    public CompletableFuture<Void> sendUserRegistrationEmailAsync(String toEmail, String username, Long userId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(toEmail)
                .subject("欢迎注册云商城")
                .content("欢迎 " + username + " 注册云商城，开始您的购物之旅！")
                .templateCode("USER_REGISTRATION")
                .userId(userId)
                .businessType("USER_REGISTRATION")
                .businessId(userId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                NOTIFICATION_BINDING_NAME,
                event,
                "USER_REGISTRATION",
                "USER_REG_" + event.getNotificationId(),
                "USER_NOTIFICATION"
        );

        log.debug("用户注册邮件通知已发送: {}", toEmail);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送密码重置邮件通知
     */
    @Async("userNotificationExecutor")
    public CompletableFuture<Void> sendPasswordResetEmailAsync(String toEmail, String resetCode, Long userId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(toEmail)
                .subject("密码重置验证码")
                .content("您的密码重置验证码是: " + resetCode + "，请在10分钟内使用。")
                .templateCode("PASSWORD_RESET")
                .userId(userId)
                .businessType("PASSWORD_RESET")
                .businessId(userId.toString())
                .priority("HIGH")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(
                NOTIFICATION_BINDING_NAME,
                event,
                "PASSWORD_RESET",
                "PWD_RESET_" + event.getNotificationId(),
                "USER_NOTIFICATION",
                3
        ).thenApply(success -> {
            if (success) {
                log.info("密码重置邮件通知发送成功: {}", toEmail);
            } else {
                log.error("密码重置邮件通知发送失败: {}", toEmail);
            }
            return null;
        });
    }

    /**
     * 异步发送用户信息变更通知
     */
    @Async("userNotificationExecutor")
    public CompletableFuture<Void> sendUserInfoChangeNotificationAsync(String toEmail, String changeType, Long userId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(toEmail)
                .subject("账户信息变更通知")
                .content("您的账户" + changeType + "已成功变更，如非本人操作请及时联系客服。")
                .templateCode("USER_INFO_CHANGE")
                .userId(userId)
                .businessType("USER_INFO_CHANGE")
                .businessId(userId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                NOTIFICATION_BINDING_NAME,
                event,
                "USER_INFO_CHANGE",
                "USER_CHANGE_" + event.getNotificationId(),
                "USER_NOTIFICATION"
        );

        log.debug("用户信息变更通知已发送: {}", toEmail);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送短信通知
     */
    @Async("userNotificationExecutor")
    public CompletableFuture<Void> sendSmsNotificationAsync(String phone, String content, String templateCode, Long userId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("SMS")
                .recipient(phone)
                .content(content)
                .templateCode(templateCode)
                .userId(userId)
                .businessType("SMS_NOTIFICATION")
                .businessId(userId.toString())
                .priority("HIGH")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(
                NOTIFICATION_BINDING_NAME,
                event,
                "SMS_NOTIFICATION",
                "SMS_" + event.getNotificationId(),
                "USER_NOTIFICATION",
                2
        ).thenApply(success -> {
            if (success) {
                log.info("短信通知发送成功: {}", StringUtils.maskPhone(phone));
            } else {
                log.error("短信通知发送失败: {}", StringUtils.maskPhone(phone));
            }
            return null;
        });
    }

    /**
     * 异步发送站内消息通知
     */
    @Async("userNotificationExecutor")
    public CompletableFuture<Void> sendInAppNotificationAsync(Long userId, String title, String content, String businessType, String businessId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("IN_APP")
                .recipient(userId.toString())
                .subject(title)
                .content(content)
                .userId(userId)
                .businessType(businessType)
                .businessId(businessId)
                .priority("LOW")
                .sendTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                NOTIFICATION_BINDING_NAME,
                event,
                "IN_APP_NOTIFICATION",
                "INAPP_" + event.getNotificationId(),
                "USER_NOTIFICATION"
        );

        log.debug("站内消息通知已发送: userId={}", userId);
        return CompletableFuture.completedFuture(null);
    }
}
