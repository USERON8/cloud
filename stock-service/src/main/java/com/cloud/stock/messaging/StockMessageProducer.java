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







@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageProducer {

    private final StreamBridge streamBridge;

    








    public boolean sendStockFreezeFailedEvent(Long orderId, String orderNo, String reason) {
        try {
            
            StockFreezeFailedEvent event = StockFreezeFailedEvent.builder()
                    .orderId(orderId)
                    .orderNo(orderNo)
                    .reason(reason)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "STOCK_FREEZE_FAILED");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", "STOCK_FREEZE_FAILED");

            
            Message<StockFreezeFailedEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            
            boolean result = streamBridge.send("stockFreezeFailedProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("鉂?搴撳瓨鍐荤粨澶辫触浜嬩欢鍙戦€佸け璐? orderId={}, orderNo={}, reason={}",
                        orderId, orderNo, reason);
            }

            return result;

        } catch (Exception e) {
            log.error("鉂?鍙戦€佸簱瀛樺喕缁撳け璐ヤ簨浠跺紓甯? orderId={}, orderNo={}, reason={}",
                    orderId, orderNo, reason, e);
            return false;
        }
    }
}
