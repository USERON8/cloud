package com.cloud.user.messaging;

import com.cloud.common.domain.event.StatisticsEvent;
import com.cloud.common.messaging.AsyncMessageProducer;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务异步统计生产者
 * 专门用于用户行为相关的统计数据发送
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncStatisticsProducer {

    private static final String STATISTICS_BINDING_NAME = "statisticsProducer-out-0";
    private final AsyncMessageProducer asyncMessageProducer;

    /**
     * 异步发送用户注册统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserRegistrationStatisticsAsync(Long userId, String registrationSource,
                                                                       String userAgent, String ipAddress, String referrer) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_REGISTRATION")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .source(registrationSource)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .referrer(referrer)
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_REGISTRATION",
                "USER_REG_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户注册统计已发送: userId={}, source={}", userId, registrationSource);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户登录统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserLoginStatisticsAsync(Long userId, String loginType, String deviceType,
                                                                String userAgent, String ipAddress, boolean success) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_LOGIN")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .actionType(loginType) // PASSWORD, SMS, OAUTH, etc.
                .deviceType(deviceType)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .remark(success ? "登录成功" : "登录失败")
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_LOGIN",
                "USER_LOGIN_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户登录统计已发送: userId={}, loginType={}, success={}", userId, loginType, success);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户行为统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserBehaviorStatisticsAsync(Long userId, String behaviorType, String targetType,
                                                                   String targetId, String source, String userAgent) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_BEHAVIOR")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .actionType(behaviorType) // VIEW, CLICK, SEARCH, FAVORITE, etc.
                .remark(targetType + ":" + targetId)
                .source(source)
                .userAgent(userAgent)
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_BEHAVIOR",
                "USER_BEHAVIOR_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户行为统计已发送: userId={}, behavior={}, target={}:{}", userId, behaviorType, targetType, targetId);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户信息变更统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserInfoChangeStatisticsAsync(Long userId, String changeType, String fieldName,
                                                                     String beforeValue, String afterValue) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_INFO_CHANGE")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .actionType(changeType) // UPDATE, DELETE, etc.
                .remark(fieldName + ": " + beforeValue + " → " + afterValue)
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_INFO_CHANGE",
                "USER_INFO_CHANGE_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户信息变更统计已发送: userId={}, field={}, {}→{}", userId, fieldName, beforeValue, afterValue);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户偏好统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserPreferenceStatisticsAsync(Long userId, String preferenceType, String preferenceValue,
                                                                     String source, Integer weight) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_PREFERENCE")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .actionType(preferenceType) // CATEGORY, BRAND, PRICE_RANGE, etc.
                .source(source)
                .remark(preferenceValue + " (权重:" + weight + ")")
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_PREFERENCE",
                "USER_PREFERENCE_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户偏好统计已发送: userId={}, type={}, value={}", userId, preferenceType, preferenceValue);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户活跃度统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserActivityStatisticsAsync(Long userId, String activityType, Integer duration,
                                                                   Integer pageViews, String sessionId) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_ACTIVITY")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .actionType(activityType) // SESSION_START, SESSION_END, PAGE_VIEW, etc.
                .duration(duration != null ? duration.longValue() : null) // 会话时长（秒）
                .quantity(pageViews) // 页面浏览数
                .sessionId(sessionId)
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_ACTIVITY",
                "USER_ACTIVITY_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户活跃度统计已发送: userId={}, activity={}, duration={}s", userId, activityType, duration);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送用户设备统计
     */
    @Async("userStatisticsExecutor")
    public CompletableFuture<Void> sendUserDeviceStatisticsAsync(Long userId, String deviceType, String deviceModel,
                                                                 String osType, String osVersion, String browserType, String browserVersion) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("USER_DEVICE")
                .businessType("USER")
                .businessId(userId.toString())
                .userId(userId)
                .deviceType(deviceType) // MOBILE, DESKTOP, TABLET
                .operatingSystem(osType + " " + osVersion)
                .browser(browserType + " " + browserVersion)
                .eventTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                STATISTICS_BINDING_NAME,
                event,
                "USER_DEVICE",
                "USER_DEVICE_STAT_" + event.getEventId(),
                "USER_STATISTICS"
        );

        log.debug("用户设备统计已发送: userId={}, device={}, os={}", userId, deviceType, osType);
        return CompletableFuture.completedFuture(null);
    }
}
