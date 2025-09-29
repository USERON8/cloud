package com.cloud.stock.messaging.consumer;

import com.cloud.common.domain.event.stock.StockReserveEvent;
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
 * 库存预扣减事件消费者
 * 负责消费库存预扣减事件，执行库存预扣减操作
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockReserveEventConsumer {

    private final StockService stockService;

    /**
     * 库存预扣减消费者函数
     * 对应绑定名称: stockReserve-in-0
     */
    @Bean("stockReserveMessageConsumer")
    public Consumer<Message<StockReserveEvent>> stockReserveConsumer() {
        return message -> {
            try {
                StockReserveEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到库存预扣减消息 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                        orderId, orderNo, event.getReserveItems().size(), traceId);

                // 1. 幂等性检查
                if (stockService.isStockReserved(orderId)) {
                    log.warn("⚠️ 库存已预扣减，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 执行库存预扣减
                boolean reserved = stockService.reserveStock(event);

                if (reserved) {
                    log.info("✅ 库存预扣减成功 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                            orderId, orderNo, event.getReserveItems().size(), traceId);
                } else {
                    log.error("❌ 库存预扣减失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("库存预扣减失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理库存预扣减消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理库存预扣减消息异常", e);
            }
        };
    }
}
