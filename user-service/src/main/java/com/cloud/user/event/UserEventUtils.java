package com.cloud.user.event;

import com.cloud.user.module.entity.User;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 函数式用户事件工具类
 * 提供常用的事件发布场景和条件判断
 *
 * @author what's up
 * @since 2025-09-20
 */
@Slf4j
@UtilityClass
public class UserEventUtils {

    // 常用的用户状态谓词
    public static final Predicate<User> IS_ACTIVE = user -> user.getStatus() == 1;
    public static final Predicate<User> IS_INACTIVE = user -> user.getStatus() == 0;
    public static final Predicate<User> IS_ADMIN = user -> "ADMIN".equals(user.getUserType());
    public static final Predicate<User> IS_MERCHANT = user -> "MERCHANT".equals(user.getUserType());
    public static final Predicate<User> IS_OAUTH_USER = user -> user.getOauthProvider() != null;
    public static final Predicate<User> IS_GITHUB_USER = user -> "github".equals(user.getOauthProvider());

    // 用户变更检测谓词
    public static final BiPredicate<User, User> STATUS_CHANGED =
            (oldUser, newUser) -> !oldUser.getStatus().equals(newUser.getStatus());

    public static final BiPredicate<User, User> USERNAME_CHANGED =
            (oldUser, newUser) -> !oldUser.getUsername().equals(newUser.getUsername());

    public static final BiPredicate<User, User> EMAIL_CHANGED =
            (oldUser, newUser) -> !Optional.ofNullable(oldUser.getEmail())
                    .equals(Optional.ofNullable(newUser.getEmail()));

    /**
     * 安全的用户事件发布 - 自动处理空值
     */
    public static void safePublishEvent(UserEventProducer publisher, User user,
                                        UserEventProducer.EventType eventType) {
        Optional.ofNullable(publisher)
                .filter(p -> user != null)
                .ifPresentOrElse(
                        p -> p.publishEvent(user, eventType),
                        () -> log.warn("🚫 跳过事件发布 - 发布器或用户为空, 事件: {}",
                                eventType != null ? eventType.getDescription() : "unknown")
                );
    }

    /**
     * 条件事件发布 - 仅在满足条件时发布
     */
    public static void conditionalPublish(UserEventProducer publisher, User user,
                                          UserEventProducer.EventType eventType,
                                          Predicate<User> condition) {
        Optional.ofNullable(user)
                .filter(condition)
                .ifPresentOrElse(
                        u -> safePublishEvent(publisher, u, eventType),
                        () -> log.debug("🔍 条件不满足，跳过事件发布 - 用户ID: {}, 事件: {}",
                                user != null ? user.getId() : "null",
                                eventType != null ? eventType.getDescription() : "unknown")
                );
    }

    /**
     * 批量用户事件发布 - 带过滤条件
     */
    public static void batchPublishWithFilter(UserEventProducer publisher, List<User> users,
                                              UserEventProducer.EventType eventType,
                                              Predicate<User> filter) {
        Optional.ofNullable(users)
                .map(List::stream)
                .orElse(Stream.empty())
                .filter(java.util.Objects::nonNull)
                .filter(filter)
                .forEach(user -> safePublishEvent(publisher, user, eventType));
    }

    /**
     * 智能用户更新事件发布 - 自动检测变更类型
     */
    public static void smartPublishUpdate(UserEventProducer publisher,
                                          User oldUser, User newUser) {
        if (publisher == null || oldUser == null || newUser == null) {
            log.warn("🚫 跳过智能更新事件发布 - 参数不完整");
            return;
        }

        // 检查状态变更
        if (STATUS_CHANGED.test(oldUser, newUser)) {
            publisher.produceStatusChanged(newUser, oldUser.getStatus());
            return;
        }

        // 检查OAuth登录
        if (IS_OAUTH_USER.test(newUser) && !IS_OAUTH_USER.test(oldUser)) {
            String provider = Optional.ofNullable(newUser.getOauthProvider()).orElse("unknown");
            publisher.publishOAuthLogin(newUser, provider);
            return;
        }

        // 常规更新
        publisher.produceUpdated(newUser);
    }

    /**
     * 用户登录事件发布 - 区分OAuth和常规登录
     */
    public static void publishLoginEvent(UserEventProducer publisher, User user,
                                         String loginType) {
        if (publisher == null || user == null) {
            log.warn("🚫 跳过登录事件发布 - 参数不完整");
            return;
        }

        switch (Optional.ofNullable(loginType).orElse("normal").toLowerCase()) {
            case "oauth", "github", "wechat", "qq" -> publisher.publishOAuthLogin(user, loginType);
            default -> publisher.produceLogin(user);
        }
    }

    /**
     * 用户删除事件发布 - 区分软删除和硬删除
     */
    public static void publishDeleteEvent(UserEventProducer publisher, User user,
                                          boolean isSoftDelete) {
        if (publisher == null || user == null) {
            log.warn("🚫 跳过删除事件发布 - 参数不完整");
            return;
        }

        String metadata = String.format("{\"删除类型\":\"%s\"}",
                isSoftDelete ? "soft" : "hard");
        publisher.publishEvent(user, UserEventProducer.EventType.DELETED, metadata);
    }

    /**
     * 高优先级用户事件检测 - 管理员和商家的操作
     */
    public static boolean isHighPriorityUser(User user) {
        return Optional.ofNullable(user)
                .map(u -> IS_ADMIN.test(u) || IS_MERCHANT.test(u))
                .orElse(false);
    }

    /**
     * 发布高优先级用户事件
     */
    public static void publishHighPriorityEvent(UserEventProducer publisher, User user,
                                                UserEventProducer.EventType eventType) {
        if (isHighPriorityUser(user)) {
            String metadata = String.format("{\"priority\":\"high\",\"userType\":\"%s\"}",
                    user.getUserType());
            publisher.publishEvent(user, eventType, metadata);
            log.info("⚡ 发布高优先级用户事件 - 用户ID: {}, 类型: {}, 事件: {}",
                    user.getId(), user.getUserType(), eventType.getDescription());
        } else {
            safePublishEvent(publisher, user, eventType);
        }
    }

    /**
     * 异常安全的事件发布包装器
     */
    public static void safeExecuteEvent(Runnable eventAction, String eventDescription) {
        try {
            eventAction.run();
        } catch (Exception e) {
            log.error("💥 事件发布异常 - {}: {}", eventDescription, e.getMessage(), e);
        }
    }
}
