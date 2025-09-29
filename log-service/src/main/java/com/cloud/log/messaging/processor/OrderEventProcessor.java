package com.cloud.log.messaging.processor;

import com.cloud.common.domain.event.order.OrderChangeEvent;
import com.cloud.common.utils.MessageUtils;
import com.cloud.log.domain.document.OrderEventDocument;
import com.cloud.log.service.OrderEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单事件处理器
 * 负责处理订单事件并存储到Elasticsearch
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final OrderEventService orderEventService;

    /**
     * 处理订单事件
     */
    public void processOrderEvent(OrderChangeEvent event, MessageHeaders headers) {
        try {
            // 获取消息头信息
            String traceId = (String) headers.get("traceId");
            String eventType = (String) headers.get("eventType");
            String tag = (String) headers.get("rocketmq_TAGS");
            Long timestamp = (Long) headers.get("timestamp");

            // 记录消息消费日志
            MessageUtils.logMessageReceive("order-events", event, traceId);

            // 幂等性检查
            if (orderEventService.existsByOrderIdAndEventType(event.getOrderId(), eventType, traceId)) {
                log.warn("⚠️ 订单事件已处理，跳过重复处理 - 订单ID: {}, 事件类型: {}, TraceId: {}",
                        event.getOrderId(), eventType, traceId);
                return;
            }

            // 构建Elasticsearch文档
            OrderEventDocument document = buildOrderEventDocument(event, headers);

            // 敏感信息脱敏
            sanitizeSensitiveData(document);

            // 保存到Elasticsearch
            orderEventService.saveOrderEvent(document);

            log.info("📁 订单事件已存储到ES - 订单ID: {}, 事件类型: {}, 文档ID: {}, TraceId: {}",
                    event.getOrderId(), eventType, document.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 处理订单事件失败 - 订单ID: {}, 错误: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("订单事件处理异常", e);
        }
    }

    /**
     * 构建订单事件文档
     */
    private OrderEventDocument buildOrderEventDocument(OrderChangeEvent event, MessageHeaders headers) {
        String traceId = (String) headers.get("traceId");
        String eventType = (String) headers.get("eventType");
        String tag = (String) headers.get("rocketmq_TAGS");
        Long timestamp = (Long) headers.get("timestamp");

        return OrderEventDocument.builder()
                .id(generateDocumentId(event.getOrderId(), eventType, traceId))
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .eventType(eventType)
                .tag(tag)
                .traceId(traceId)
                .totalAmount(event.getTotalAmount())
                .paidAmount(event.getPayAmount())
                .orderStatus(event.getAfterStatus())
                .oldOrderStatus(event.getBeforeStatus())
                .operatorName(event.getOperator())
                .eventTime(event.getOperateTime() != null ? event.getOperateTime() : LocalDateTime.now())
                .messageTimestamp(timestamp != null ? timestamp : System.currentTimeMillis())
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 敏感信息脱敏（简化版）
     */
    private void sanitizeSensitiveData(OrderEventDocument document) {
        // 对于简化的事件模型，暂时不需要脱敏处理
        // 可以根据实际需要后续添加
    }

    /**
     * 生成文档ID
     */
    private String generateDocumentId(Long orderId, String eventType, String traceId) {
        return String.format("order_%d_%s_%s_%d", orderId, eventType, traceId, System.currentTimeMillis());
    }
}
