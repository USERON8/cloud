package com.cloud.user.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "user.notification.delivery-provider",
        havingValue = "redis",
        matchIfMissing = true
)
public class RedisUserNotificationDeliveryProvider implements UserNotificationDeliveryProvider {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean deliverWelcome(Long userId) {
        return setValue("notification:welcome:" + userId, System.currentTimeMillis());
    }

    @Override
    public boolean deliverPasswordResetToken(Long userId, String resetToken) {
        return setValueWithTtl("password:reset:token:" + resetToken, userId, 24, TimeUnit.HOURS);
    }

    @Override
    public boolean deliverActivationToken(Long userId, String activationToken) {
        return setValueWithTtl("user:activation:token:" + activationToken, userId, 48, TimeUnit.HOURS);
    }

    @Override
    public boolean deliverStatusChange(Long userId, Integer newStatus, String reason) {
        String key = "notification:status_change:" + userId + ":" + System.currentTimeMillis();
        String payload = "status=" + newStatus + ";reason=" + (reason == null ? "" : reason);
        return setValue(key, payload);
    }

    @Override
    public boolean deliverBatchNotification(Long userId, String title, String content) {
        String key = "notification:batch:" + userId + ":" + System.currentTimeMillis();
        String payload = "title=" + title + ";content=" + content;
        return setValue(key, payload);
    }

    @Override
    public boolean deliverSystemAnnouncement(String title, String content) {
        String key = "notification:system:" + System.currentTimeMillis();
        String payload = "title=" + title + ";content=" + content;
        return setValue(key, payload);
    }

    private boolean setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish notification payload, key={}", key, e);
            return false;
        }
    }

    private boolean setValueWithTtl(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish notification payload with ttl, key={}", key, e);
            return false;
        }
    }
}
