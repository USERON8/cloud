package com.cloud.stock.messaging.consumer;

import com.cloud.common.domain.event.stock.StockConfirmEvent;
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
 * 库存确认扣减事件消费者
 * 负责消费库存确认扣减事件，将预扣减的库存转为正式扣减
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockConfirmEventConsumer {

    private final StockService stockService;

    /**
     * 库存确认扣减消费者函数
     * 对应绑定名称: stockConfirm-in-0
     */
    @Bean("stockConfirmMessageConsumer")
    public Consumer<Message<StockConfirmEvent>> stockConfirmConsumer() {
        return message -> {
            try {
                StockConfirmEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到库存确认扣减消息 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                        orderId, orderNo, event.getConfirmItems().size(), traceId);

                // 1. 幂等性检查
                if (stockService.isStockConfirmed(orderId)) {
                    log.warn("⚠️ 库存已确认扣减，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 执行库存确认扣减
                boolean confirmed = stockService.confirmStock(event);

                if (confirmed) {
                    log.info("✅ 库存确认扣减成功 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                            orderId, orderNo, event.getConfirmItems().size(), traceId);
                } else {
                    log.error("❌ 库存确认扣减失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("库存确认扣减失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理库存确认扣减消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理库存确认扣减消息异常", e);
            }
        };
    }
}
