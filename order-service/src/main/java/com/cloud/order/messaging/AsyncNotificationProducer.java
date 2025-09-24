package com.cloud.order.messaging;

import com.cloud.common.domain.event.NotificationEvent;
import com.cloud.common.messaging.AsyncMessageProducer;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 订单服务异步通知生产者
 * 专门用于订单相关的通知消息发送
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
     * 异步发送订单创建通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderCreatedNotificationAsync(Long userId, String userEmail, Long orderId, BigDecimal totalAmount) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单创建成功")
                .content("您的订单 " + orderId + " 已创建成功，订单金额: ¥" + totalAmount + "，请及时完成支付。")
                .templateCode("ORDER_CREATED")
                .userId(userId)
                .businessType("ORDER_CREATED")
                .businessId(orderId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(NOTIFICATION_BINDING_NAME, event,
                        "ORDER_CREATED", "ORDER_CREATED_" + orderId, "ORDER_NOTIFICATION", 2)
                .thenRun(() -> log.info("订单创建通知发送成功: orderId={}, userId={}", orderId, userId))
                .exceptionally(throwable -> {
                    log.error("订单创建通知发送失败: orderId={}, userId={}, 错误: {}", orderId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送订单支付成功通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderPaidNotificationAsync(Long userId, String userEmail, Long orderId, BigDecimal paidAmount) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单支付成功")
                .content("您的订单 " + orderId + " 支付成功，支付金额: ¥" + paidAmount + "，我们将尽快为您发货。")
                .templateCode("ORDER_PAID")
                .userId(userId)
                .businessType("ORDER_PAID")
                .businessId(orderId.toString())
                .priority("HIGH")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(NOTIFICATION_BINDING_NAME, event,
                        "ORDER_PAID", "ORDER_PAID_" + orderId, "ORDER_NOTIFICATION", 3)
                .thenRun(() -> log.info("订单支付成功通知发送成功: orderId={}, userId={}", orderId, userId))
                .exceptionally(throwable -> {
                    log.error("订单支付成功通知发送失败: orderId={}, userId={}, 错误: {}", orderId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送订单发货通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderShippedNotificationAsync(Long userId, String userEmail, Long orderId, String trackingNumber) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单已发货")
                .content("您的订单 " + orderId + " 已发货，快递单号: " + trackingNumber + "，请注意查收。")
                .templateCode("ORDER_SHIPPED")
                .userId(userId)
                .businessType("ORDER_SHIPPED")
                .businessId(orderId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(NOTIFICATION_BINDING_NAME, event,
                        "ORDER_SHIPPED", "ORDER_SHIPPED_" + orderId, "ORDER_NOTIFICATION", 2)
                .thenRun(() -> log.info("订单发货通知发送成功: orderId={}, userId={}", orderId, userId))
                .exceptionally(throwable -> {
                    log.error("订单发货通知发送失败: orderId={}, userId={}, 错误: {}", orderId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送订单取消通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderCancelledNotificationAsync(Long userId, String userEmail, Long orderId, String cancelReason) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单已取消")
                .content("您的订单 " + orderId + " 已取消，取消原因: " + cancelReason + "，如有疑问请联系客服。")
                .templateCode("ORDER_CANCELLED")
                .userId(userId)
                .businessType("ORDER_CANCELLED")
                .businessId(orderId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(NOTIFICATION_BINDING_NAME, event,
                "ORDER_CANCELLED", "ORDER_CANCELLED_" + orderId, "ORDER_NOTIFICATION");

        log.info("订单取消通知已提交发送: orderId={}, userId={}", orderId, userId);

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送订单退款通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderRefundedNotificationAsync(Long userId, String userEmail, Long orderId, BigDecimal refundAmount) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单退款成功")
                .content("您的订单 " + orderId + " 退款成功，退款金额: ¥" + refundAmount + "，款项将在3-5个工作日内到账。")
                .templateCode("ORDER_REFUNDED")
                .userId(userId)
                .businessType("ORDER_REFUNDED")
                .businessId(orderId.toString())
                .priority("HIGH")
                .sendTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsyncWithRetry(NOTIFICATION_BINDING_NAME, event,
                        "ORDER_REFUNDED", "ORDER_REFUNDED_" + orderId, "ORDER_NOTIFICATION", 3)
                .thenRun(() -> log.info("订单退款通知发送成功: orderId={}, userId={}", orderId, userId))
                .exceptionally(throwable -> {
                    log.error("订单退款通知发送失败: orderId={}, userId={}, 错误: {}", orderId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送订单超时未支付通知
     */
    @Async("orderNotificationExecutor")
    public CompletableFuture<Void> sendOrderPaymentTimeoutNotificationAsync(Long userId, String userEmail, Long orderId) {

        NotificationEvent event = NotificationEvent.builder()
                .notificationId(StringUtils.generateTraceId())
                .notificationType("EMAIL")
                .recipient(userEmail)
                .subject("订单支付超时提醒")
                .content("您的订单 " + orderId + " 即将超时，请尽快完成支付，否则订单将被自动取消。")
                .templateCode("ORDER_PAYMENT_TIMEOUT")
                .userId(userId)
                .businessType("ORDER_PAYMENT_TIMEOUT")
                .businessId(orderId.toString())
                .priority("NORMAL")
                .sendTime(LocalDateTime.now())
                .build();

        asyncMessageProducer.sendAsyncSilent(NOTIFICATION_BINDING_NAME, event,
                "ORDER_PAYMENT_TIMEOUT", "ORDER_TIMEOUT_" + orderId, "ORDER_NOTIFICATION");

        log.info("订单支付超时通知已提交发送: orderId={}, userId={}", orderId, userId);

        return CompletableFuture.completedFuture(null);
    }
}
