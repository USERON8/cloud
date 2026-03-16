package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "stock-freeze-failed",
        consumerGroup = "order-stock-freeze-failed-consumer-group",
        selectorExpression = "STOCK_FREEZE_FAILED")
public class StockFreezeFailedConsumer extends AbstractMqConsumer<StockFreezeFailedEvent> {

    private static final String NS_STOCK_FREEZE_FAILED = "order:stock:freeze-failed";

    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;
    private final OrderService orderService;
    private final TradeMetrics tradeMetrics;
    private final ObjectMapper objectMapper;

    @Override
    protected void doConsume(StockFreezeFailedEvent event, MessageExt msgExt) {
        if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
            tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
            return;
        }
        OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
        if (mainOrder == null) {
            tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
            return;
        }
        List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
        for (OrderSub subOrder : subOrders) {
            if (subOrder == null) {
                continue;
            }
            String status = subOrder.getOrderStatus();
            if (!"CANCELLED".equals(status) && !"CLOSED".equals(status)) {
                orderService.advanceSubOrderStatus(subOrder.getId(), "CANCEL");
            }
        }
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "success");
    }

    @Override
    protected StockFreezeFailedEvent deserialize(byte[] body) {
        try {
            return body == null ? null : objectMapper.readValue(body, StockFreezeFailedEvent.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize StockFreezeFailedEvent", ex);
        }
    }

    @Override
    protected String resolveIdempotentNamespace(
            String topic, MessageExt msgExt, StockFreezeFailedEvent payload) {
        return NS_STOCK_FREEZE_FAILED;
    }

    @Override
    protected String buildIdempotentKey(
            String topic, String msgId, StockFreezeFailedEvent payload, MessageExt msgExt) {
        return resolveEventId(payload);
    }

    @Override
    protected void onBizException(MessageExt msgExt, StockFreezeFailedEvent payload, com.cloud.common.exception.BizException ex) {
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "biz");
    }

    @Override
    protected void onSystemException(
            MessageExt msgExt,
            StockFreezeFailedEvent payload,
            com.cloud.common.exception.SystemException ex,
            boolean retryable) {
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", retryable ? "retry" : "failed");
    }

    @Override
    protected void onUnknownException(MessageExt msgExt, StockFreezeFailedEvent payload, Exception ex) {
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "retry");
    }

    private String resolveEventId(StockFreezeFailedEvent event) {
        if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        if (event != null && event.getOrderNo() != null && !event.getOrderNo().isBlank()) {
            return "STOCK_FREEZE_FAILED:" + event.getOrderNo();
        }
        return "STOCK_FREEZE_FAILED:" + System.currentTimeMillis();
    }
}
