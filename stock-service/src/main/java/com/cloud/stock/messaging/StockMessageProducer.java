package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.StockFreezeFailedEvent;
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
 * 库存消息生产者
 * 发送库存相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送库存冻结失败事件
     * 通知订单服务取消订单
     *
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param reason  失败原因
     * @return 是否发送成功
     */
    public boolean sendStockFreezeFailedEvent(Long orderId, String orderNo, String reason) {
        try {
            // 构建事件
            StockFreezeFailedEvent event = StockFreezeFailedEvent.builder()
                    .orderId(orderId)
                    .orderNo(orderNo)
                    .reason(reason)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "STOCK_FREEZE_FAILED");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", "STOCK_FREEZE_FAILED");

            // 构建消息
            Message<StockFreezeFailedEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            // 发送到stock-freeze-failed topic
            boolean result = streamBridge.send("stockFreezeFailedProducer-out-0", message);

            if (result) {
                log.info("✅ 库存冻结失败事件发送成功: orderId={}, orderNo={}, reason={}, eventId={}",
                        orderId, orderNo, reason, event.getEventId());
            } else {
                log.error("❌ 库存冻结失败事件发送失败: orderId={}, orderNo={}, reason={}",
                        orderId, orderNo, reason);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送库存冻结失败事件异常: orderId={}, orderNo={}, reason={}",
                    orderId, orderNo, reason, e);
            return false;
        }
    }
}
