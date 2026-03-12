package com.cloud.order.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompletedConsumer {

    private static final String NS_REFUND_COMPLETED = "order:refund:completed";

    private final MessageIdempotencyService messageIdempotencyService;
    private final AfterSaleMapper afterSaleMapper;
    private final OrderSubMapper orderSubMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final OrderMessageProducer orderMessageProducer;
    private final TradeMetrics tradeMetrics;

    @Bean
    public Consumer<Message<RefundCompletedEvent>> refundCompletedConsumer() {
        return message -> {
            RefundCompletedEvent event = message.getPayload();
            String eventId = getEventId(event);
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_COMPLETED, eventId)) {
                log.warn("Duplicate refund-completed event, skip: eventId={}", eventId);
                return;
            }

            try {
                if (event == null || event.getAfterSaleNo() == null || event.getAfterSaleNo().isBlank()) {
                    tradeMetrics.incrementMessageConsume("refund_completed", "failed");
                    messageIdempotencyService.markSuccess(NS_REFUND_COMPLETED, eventId);
                    return;
                }

                AfterSale afterSale = afterSaleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getAfterSaleNo, event.getAfterSaleNo())
                        .eq(AfterSale::getDeleted, 0)
                        .last("LIMIT 1"));
                if (afterSale == null) {
                    tradeMetrics.incrementMessageConsume("refund_completed", "failed");
                    messageIdempotencyService.markSuccess(NS_REFUND_COMPLETED, eventId);
                    return;
                }

                if (!"REFUNDED".equals(afterSale.getStatus())) {
                    orderService.advanceAfterSaleStatus(afterSale.getId(), "REFUND", "refund completed");
                }

                StockRestoreEvent restoreEvent = buildStockRestoreEvent(afterSale, event);
                if (restoreEvent != null) {
                    orderMessageProducer.sendStockRestoreEvent(restoreEvent);
                }

                tradeMetrics.incrementMessageConsume("refund_completed", "success");
                messageIdempotencyService.markSuccess(NS_REFUND_COMPLETED, eventId);
            } catch (Exception ex) {
                tradeMetrics.incrementMessageConsume("refund_completed", "retry");
                log.error("Handle refund-completed event failed: eventId={}, afterSaleNo={}", eventId,
                        event == null ? null : event.getAfterSaleNo(), ex);
                throw new RuntimeException("Handle refund-completed event failed", ex);
            }
        };
    }

    private StockRestoreEvent buildStockRestoreEvent(AfterSale afterSale, RefundCompletedEvent event) {
        if (afterSale.getSubOrderId() == null) {
            return null;
        }
        OrderSub subOrder = orderSubMapper.selectById(afterSale.getSubOrderId());
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            return null;
        }
        List<OrderItem> items = orderItemMapper.listActiveBySubOrderId(subOrder.getId());
        if (items == null || items.isEmpty()) {
            return null;
        }

        Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getSkuId() == null || item.getQuantity() == null) {
                continue;
            }
            skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }
        if (skuQuantities.isEmpty()) {
            return null;
        }

        List<StockOperateCommandDTO> commands = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
            StockOperateCommandDTO command = new StockOperateCommandDTO();
            command.setSubOrderNo(subOrder.getSubOrderNo());
            command.setSkuId(entry.getKey());
            command.setQuantity(entry.getValue());
            command.setReason("refund restore " + afterSale.getAfterSaleNo());
            commands.add(command);
        }

        return StockRestoreEvent.builder()
                .refundNo(event.getRefundNo())
                .mainOrderNo(event.getMainOrderNo())
                .subOrderNo(subOrder.getSubOrderNo())
                .items(commands)
                .build();
    }

    private String getEventId(RefundCompletedEvent event) {
        if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        if (event != null && event.getRefundNo() != null && !event.getRefundNo().isBlank()) {
            return "REFUND_COMPLETED:" + event.getRefundNo();
        }
        return "REFUND_COMPLETED:" + System.currentTimeMillis();
    }
}
