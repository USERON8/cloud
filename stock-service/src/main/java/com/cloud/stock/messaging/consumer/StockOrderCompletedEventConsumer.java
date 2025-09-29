package com.cloud.stock.messaging.consumer;

import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 库存订单完成事件消费者
 * 负责消费订单完成事件，解冻并扣减库存
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockOrderCompletedEventConsumer {

    private final StockService stockService;

    /**
     * 库存订单完成消费者函数
     * 对应绑定名称: stockOrderCompletedConsumer-in-0
     */
    @Bean("stockOrderCompletedMessageConsumer")
    public Consumer<Message<OrderCompletedEvent>> stockOrderCompletedConsumer() {
        return message -> {
            try {
                OrderCompletedEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到订单完成消息 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        orderId, orderNo, traceId);

                // 1. 幂等性检查
                if (stockService.isStockDeducted(orderId)) {
                    log.warn("⚠️ 库存已扣减，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 解冻并扣减库存
                boolean processed = stockService.unfreezeAndDeductStock(event);

                if (processed) {
                    log.info("✅ 库存解冻扣减成功 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                            orderId, orderNo, event.getOrderItems() != null ? event.getOrderItems().size() : 0, traceId);
                } else {
                    log.error("❌ 库存解冻扣减失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("库存解冻扣减失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理订单完成消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理订单完成消息异常", e);
            }
        };
    }
}
