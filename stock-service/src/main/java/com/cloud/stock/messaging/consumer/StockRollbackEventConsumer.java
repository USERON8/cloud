package com.cloud.stock.messaging.consumer;

import com.cloud.common.domain.event.stock.StockRollbackEvent;
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
 * 库存回滚事件消费者
 * 负责消费库存回滚事件，回滚预扣减或已扣减的库存
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockRollbackEventConsumer {

    private final StockService stockService;

    /**
     * 库存回滚消费者函数
     * 对应绑定名称: stockRollback-in-0
     */
    @Bean("stockRollbackMessageConsumer")
    public Consumer<Message<StockRollbackEvent>> stockRollbackConsumer() {
        return message -> {
            try {
                StockRollbackEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到库存回滚消息 - 订单ID: {}, 订单号: {}, 回滚类型: {}, 商品数量: {}, 追踪ID: {}",
                        orderId, orderNo, event.getRollbackType(), event.getRollbackItems().size(), traceId);

                // 1. 幂等性检查
                if (stockService.isStockRolledBack(orderId)) {
                    log.warn("⚠️ 库存已回滚，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 执行库存回滚
                boolean rolledBack = stockService.rollbackStock(event);

                if (rolledBack) {
                    log.info("✅ 库存回滚成功 - 订单ID: {}, 订单号: {}, 回滚类型: {}, 商品数量: {}, 追踪ID: {}",
                            orderId, orderNo, event.getRollbackType(), event.getRollbackItems().size(), traceId);
                } else {
                    log.error("❌ 库存回滚失败 - 订单ID: {}, 订单号: {}, 回滚类型: {}, 追踪ID: {}",
                            orderId, orderNo, event.getRollbackType(), traceId);
                    throw new MessageConsumeException("库存回滚失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理库存回滚消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理库存回滚消息异常", e);
            }
        };
    }
}
