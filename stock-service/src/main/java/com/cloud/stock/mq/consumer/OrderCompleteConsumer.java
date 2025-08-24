package com.cloud.stock.mq.consumer;

import com.cloud.common.domain.OrderCompleteEvent;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 订单完成消息消费者
 * 监听订单服务发送的订单完成消息，用于扣减库存
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompleteConsumer {

    private final StockService stockService;

    /**
     * 订单完成消息消费者
     * 当订单服务完成订单时，会发送消息到ORDER_COMPLETE_TOPIC
     * 库存服务接收到消息后，需要扣减相应商品的库存
     *
     * @return 消息消费者函数
     */
    @Bean
    public Consumer<OrderCompleteEvent> orderCompleteConsumer() {
        return event -> {
            try {
                log.info("接收到订单完成消息，订单ID: {}, 商品ID: {}, 数量: {}, 操作人: {}", 
                        event.getOrderId(), event.getProductId(), event.getQuantity(), event.getOperator());
                
                // 扣减库存，传递操作人信息
                boolean reduced = stockService.reduceStock(event.getProductId(), event.getQuantity(), event.getOperator());
                if (reduced) {
                    log.info("库存扣减成功，订单ID: {}, 商品ID: {}, 数量: {}, 操作人: {}", 
                            event.getOrderId(), event.getProductId(), event.getQuantity(), event.getOperator());
                } else {
                    log.error("库存扣减失败，订单ID: {}, 商品ID: {}, 数量: {}, 操作人: {}", 
                            event.getOrderId(), event.getProductId(), event.getQuantity(), event.getOperator());
                }
                
                log.info("订单完成消息处理完成，订单ID: {}", event.getOrderId());
            } catch (Exception e) {
                log.error("处理订单完成消息失败，订单ID: {}", event.getOrderId(), e);
                // 可以在这里实现消息重试逻辑或者发送失败通知
            }
        };
    }
}