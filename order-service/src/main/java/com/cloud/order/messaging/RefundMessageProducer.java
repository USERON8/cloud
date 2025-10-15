package com.cloud.order.messaging;

import com.cloud.order.module.entity.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 退款消息生产者
 * 发送退款相关的事件消息
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送退款创建事件
     * 通知商家有新的退款申请
     *
     * @param refund 退款单
     * @return 是否发送成功
     */
    public boolean sendRefundCreatedEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_CREATED");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_CREATED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundCreatedProducer-out-0", message);

            if (result) {
                log.info("✅ 退款创建事件发送成功: refundId={}, refundNo={}",
                        refund.getId(), refund.getRefundNo());
            } else {
                log.error("❌ 退款创建事件发送失败: refundId={}, refundNo={}",
                        refund.getId(), refund.getRefundNo());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送退款创建事件异常: refundId={}, refundNo={}",
                    refund.getId(), refund.getRefundNo(), e);
            return false;
        }
    }

    /**
     * 发送退款审核事件
     * 通知用户退款审核结果
     *
     * @param refund   退款单
     * @param approved 是否通过
     * @return 是否发送成功
     */
    public boolean sendRefundAuditedEvent(Refund refund, Boolean approved) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_AUDITED");
            payload.put("approved", approved);

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_AUDITED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundAuditedProducer-out-0", message);

            if (result) {
                log.info("✅ 退款审核事件发送成功: refundId={}, approved={}",
                        refund.getId(), approved);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送退款审核事件异常: refundId={}", refund.getId(), e);
            return false;
        }
    }

    /**
     * 发送退款处理事件
     * 通知payment-service处理退款
     *
     * @param refund 退款单
     * @return 是否发送成功
     */
    public boolean sendRefundProcessEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_PROCESS");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_PROCESS");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundProcessProducer-out-0", message);

            if (result) {
                log.info("✅ 退款处理事件发送成功: refundId={}, refundNo={}, amount={}",
                        refund.getId(), refund.getRefundNo(), refund.getRefundAmount());
            } else {
                log.error("❌ 退款处理事件发送失败: refundId={}", refund.getId());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送退款处理事件异常: refundId={}", refund.getId(), e);
            return false;
        }
    }

    /**
     * 发送退款取消事件
     *
     * @param refund 退款单
     * @return 是否发送成功
     */
    public boolean sendRefundCancelledEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_CANCELLED");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_CANCELLED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundCancelledProducer-out-0", message);

            if (result) {
                log.info("✅ 退款取消事件发送成功: refundId={}", refund.getId());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送退款取消事件异常: refundId={}", refund.getId(), e);
            return false;
        }
    }

    /**
     * 构建退款消息载荷
     */
    private Map<String, Object> buildRefundPayload(Refund refund, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("refundId", refund.getId());
        payload.put("refundNo", refund.getRefundNo());
        payload.put("orderId", refund.getOrderId());
        payload.put("orderNo", refund.getOrderNo());
        payload.put("userId", refund.getUserId());
        payload.put("merchantId", refund.getMerchantId());
        payload.put("refundType", refund.getRefundType());
        payload.put("refundAmount", refund.getRefundAmount());
        payload.put("refundReason", refund.getRefundReason());
        payload.put("status", refund.getStatus());
        return payload;
    }
}
