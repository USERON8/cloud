package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
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
 * 订单消息生产者
 * 发送订单相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送订单创建事件
     * 通知库存服务冻结库存、支付服务创建支付
     *
     * @param event 订单创建事件
     * @return 是否发送成功
     */
    public boolean sendOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            // 设置事件ID和时间戳
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }

            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, event.getOrderNo());
            headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CREATED");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", "ORDER_CREATED");

            // 构建消息
            Message<OrderCreatedEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            // 发送到order-created topic
            boolean result = streamBridge.send("orderCreatedProducer-out-0", message);

            if (result) {
                log.info("✅ 订单创建事件发送成功: orderId={}, orderNo={}, eventId={}",
                        event.getOrderId(), event.getOrderNo(), event.getEventId());
            } else {
                log.error("❌ 订单创建事件发送失败: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送订单创建事件异常: orderId={}, orderNo={}",
                    event.getOrderId(), event.getOrderNo(), e);
            return false;
        }
    }

    /**
     * 发送订单取消事件
     *
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param reason  取消原因
     * @return 是否发送成功
     */
    public boolean sendOrderCancelledEvent(Long orderId, String orderNo, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", orderId);
            payload.put("orderNo", orderNo);
            payload.put("reason", reason);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("eventId", UUID.randomUUID().toString());

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CANCELLED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("orderCancelledProducer-out-0", message);

            if (result) {
                log.info("✅ 订单取消事件发送成功: orderId={}, orderNo={}, reason={}",
                        orderId, orderNo, reason);
            } else {
                log.error("❌ 订单取消事件发送失败: orderId={}, orderNo={}",
                        orderId, orderNo);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送订单取消事件异常: orderId={}, orderNo={}",
                    orderId, orderNo, e);
            return false;
        }
    }
}
