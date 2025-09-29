package com.cloud.stock.messaging.consumer;

import com.cloud.common.domain.event.order.OrderCreatedEvent;
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
 * 库存订单创建事件消费者
 * 负责消费订单创建事件，冻结库存
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockOrderCreatedEventConsumer {

    private final StockService stockService;

    /**
     * 库存订单创建消费者函数
     * 对应绑定名称: stockOrderCreatedConsumer-in-0
     */
    @Bean("stockOrderCreatedMessageConsumer")
    public Consumer<Message<OrderCreatedEvent>> stockOrderCreatedConsumer() {
        return message -> {
            try {
                OrderCreatedEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到订单创建消息 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        orderId, orderNo, traceId);

                // 1. 幂等性检查
                if (stockService.isStockFrozen(orderId)) {
                    log.warn("⚠️ 库存已冻结，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 冻结库存
                boolean frozen = stockService.freezeStock(event);

                if (frozen) {
                    log.info("✅ 库存冻结成功 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                            orderId, orderNo, event.getOrderItems().size(), traceId);
                } else {
                    log.error("❌ 库存冻结失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("库存冻结失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理订单创建消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理订单创建消息异常", e);
            }
        };
    }
}
