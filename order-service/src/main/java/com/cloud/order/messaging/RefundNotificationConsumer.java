package com.cloud.order.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 退款通知消费者
 * 监听退款事件并发送通知给用户/商家
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundNotificationConsumer {

    /**
     * 消费退款创建事件 - 通知商家有新的退款申请
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> refundCreatedNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Long userId = ((Number) event.get("userId")).longValue();
            Long merchantId = event.get("merchantId") != null ? ((Number) event.get("merchantId")).longValue() : null;

            log.info("📧 [退款通知] 接收到退款创建事件: refundNo={}, orderNo={}", refundNo, orderNo);

            try {
                // 通知商家：有新的退款申请
                if (merchantId != null) {
                    sendNotification(
                            "MERCHANT",
                            merchantId,
                            "新的退款申请",
                            String.format("订单 %s 有新的退款申请，退款单号：%s，请及时处理", orderNo, refundNo)
                    );
                    log.info("✅ 已通知商家: merchantId={}, refundNo={}", merchantId, refundNo);
                }

            } catch (Exception e) {
                log.error("❌ 处理退款创建通知失败: refundNo={}", refundNo, e);
            }
        };
    }

    /**
     * 消费退款审核事件 - 通知用户审核结果
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> refundAuditedNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Boolean approved = (Boolean) event.get("approved");
            Integer status = ((Number) event.get("status")).intValue();

            log.info("📧 [退款通知] 接收到退款审核事件: refundNo={}, approved={}", refundNo, approved);

            try {
                // 通知用户审核结果
                String title = approved ? "退款申请已通过" : "退款申请已拒绝";
                String content = approved ?
                        String.format("您的退款申请（退款单号：%s）已通过商家审核，我们将尽快为您办理退款", refundNo) :
                        String.format("您的退款申请（退款单号：%s）未通过商家审核，如有疑问请联系商家", refundNo);

                Long userId = event.get("userId") != null ? ((Number) event.get("userId")).longValue() : null;
                if (userId != null) {
                    sendNotification("USER", userId, title, content);
                    log.info("✅ 已通知用户审核结果: userId={}, refundNo={}, approved={}", userId, refundNo, approved);
                }

            } catch (Exception e) {
                log.error("❌ 处理退款审核通知失败: refundNo={}", refundNo, e);
            }
        };
    }

    /**
     * 消费退款处理事件 - 通知用户退款正在处理中
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> refundProcessNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            String refundNo = (String) event.get("refundNo");
            Long userId = event.get("userId") != null ? ((Number) event.get("userId")).longValue() : null;

            log.info("📧 [退款通知] 接收到退款处理事件: refundNo={}", refundNo);

            try {
                if (userId != null) {
                    sendNotification(
                            "USER",
                            userId,
                            "退款处理中",
                            String.format("您的退款（退款单号：%s）正在处理中，请耐心等待", refundNo)
                    );
                    log.info("✅ 已通知用户退款处理中: userId={}, refundNo={}", userId, refundNo);
                }

            } catch (Exception e) {
                log.error("❌ 处理退款处理通知失败: refundNo={}", refundNo, e);
            }
        };
    }

    /**
     * 消费退款取消事件 - 通知商家用户已取消退款
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> refundCancelledNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            String refundNo = (String) event.get("refundNo");
            Long merchantId = event.get("merchantId") != null ? ((Number) event.get("merchantId")).longValue() : null;

            log.info("📧 [退款通知] 接收到退款取消事件: refundNo={}", refundNo);

            try {
                if (merchantId != null) {
                    sendNotification(
                            "MERCHANT",
                            merchantId,
                            "退款申请已取消",
                            String.format("用户已取消退款申请，退款单号：%s", refundNo)
                    );
                    log.info("✅ 已通知商家退款取消: merchantId={}, refundNo={}", merchantId, refundNo);
                }

            } catch (Exception e) {
                log.error("❌ 处理退款取消通知失败: refundNo={}", refundNo, e);
            }
        };
    }

    /**
     * 发送通知（简化实现 - 仅记录日志）
     * 实际项目中应该调用通知服务API或发送到消息队列
     *
     * @param receiverType 接收方类型
     * @param receiverId   接收方ID
     * @param title        通知标题
     * @param content      通知内容
     */
    private void sendNotification(String receiverType, Long receiverId, String title, String content) {
        // TODO: 实际项目中应该：
        // 1. 保存通知记录到refund_notification表
        // 2. 调用第三方通知服务API（短信、邮件、推送等）
        // 3. 或发送到专门的通知服务消息队列

        log.info("📬 发送通知 | 接收方：{}(ID:{}), 标题：{}, 内容：{}",
                receiverType, receiverId, title, content);

        // 简化实现：仅记录日志，表示通知已发送
        // 实际项目中需要持久化到数据库，并异步发送
    }
}
