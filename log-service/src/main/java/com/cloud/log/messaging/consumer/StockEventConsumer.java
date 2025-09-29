package com.cloud.log.messaging.consumer;

import com.cloud.common.domain.event.stock.StockChangeEvent;
import com.cloud.log.messaging.processor.StockEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 库存事件消费者
 * 负责消费库存变更事件并存储到Elasticsearch
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockEventConsumer {

    private final StockEventProcessor stockEventProcessor;

    /**
     * 库存事件消费函数
     * 使用官方标准的函数式编程模型
     */
    @Bean
    public Consumer<Message<StockChangeEvent>> stockConsumer() {
        return message -> {
            try {
                // 获取消息内容和头信息
                StockChangeEvent event = message.getPayload();
                String traceId = (String) message.getHeaders().get("traceId");
                String eventType = (String) message.getHeaders().get("eventType");
                String tag = (String) message.getHeaders().get("rocketmq_TAGS");

                log.info("🔔 接收到库存事件 - 事件类型: {}, 库存ID: {}, 商品ID: {}, 商品名称: {}, Tag: {}, TraceId: {}",
                        eventType, event.getStockId(), event.getProductId(), event.getProductName(),
                        tag, traceId);

                // 处理库存事件
                stockEventProcessor.processStockEvent(event, traceId);

                log.info("✅ 库存事件处理完成 - 库存ID: {}, 事件类型: {}, TraceId: {}",
                        event.getStockId(), eventType, traceId);

            } catch (Exception e) {
                log.error("❌ 处理库存事件时发生异常: {}", e.getMessage(), e);
                // 根据业务需要决定是否抛出异常进行重试
                throw new RuntimeException("库存事件处理失败", e);
            }
        };
    }
}
