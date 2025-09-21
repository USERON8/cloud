package com.cloud.log.messaging.processor;

import com.cloud.common.domain.event.PaymentChangeEvent;
import com.cloud.common.utils.MessageUtils;
import com.cloud.log.domain.document.PaymentEventDocument;
import com.cloud.log.service.PaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付事件处理器
 * 负责处理支付事件并存储到Elasticsearch
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final PaymentEventService paymentEventService;

    /**
     * 处理支付事件
     */
    public void processPaymentEvent(PaymentChangeEvent event, MessageHeaders headers) {
        try {
            // 获取消息头信息
            String traceId = (String) headers.get("traceId");
            String eventType = (String) headers.get("eventType");
            String tag = (String) headers.get("rocketmq_TAGS");
            Long timestamp = (Long) headers.get("timestamp");

            // 记录消息消费日志
            MessageUtils.logMessageReceive("payment-events", event, traceId);

            // 幂等性检查
            if (paymentEventService.existsByPaymentIdAndEventType(event.getPaymentId(), eventType, traceId)) {
                log.warn("⚠️ 支付事件已处理，跳过重复处理 - 支付ID: {}, 事件类型: {}, TraceId: {}",
                        event.getPaymentId(), eventType, traceId);
                return;
            }

            // 构建Elasticsearch文档
            PaymentEventDocument document = buildPaymentEventDocument(event, headers);

            // 敏感信息脱敏
            sanitizeSensitiveData(document);

            // 保存到Elasticsearch
            paymentEventService.savePaymentEvent(document);

            log.info("📁 支付事件已存储到ES - 支付ID: {}, 事件类型: {}, 文档ID: {}, TraceId: {}",
                    event.getPaymentId(), eventType, document.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 处理支付事件失败 - 支付ID: {}, 错误: {}", event.getPaymentId(), e.getMessage(), e);
            throw new RuntimeException("支付事件处理异常", e);
        }
    }

    /**
     * 构建支付事件文档
     */
    private PaymentEventDocument buildPaymentEventDocument(PaymentChangeEvent event, MessageHeaders headers) {
        String traceId = (String) headers.get("traceId");
        String eventType = (String) headers.get("eventType");
        String tag = (String) headers.get("rocketmq_TAGS");
        Long timestamp = (Long) headers.get("timestamp");

        return PaymentEventDocument.builder()
                .id(generateDocumentId(event.getPaymentId(), eventType, traceId))
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .eventType(eventType)
                .tag(tag)
                .traceId(traceId)
                .paymentMethod(Integer.valueOf(event.getPaymentMethod()))
                .paymentAmount(event.getAmount())
                .oldPaymentStatus(event.getBeforeStatus())
                .paymentStatus(event.getAfterStatus())
                .thirdPartyTxnId(event.getThirdPartyTransactionId())
                .productDescription(event.getDescription())
                .remark(event.getReason())
                .operatorId(Long.valueOf(event.getOperatorId()))
                .operatorName(event.getOperatorName())
                .paymentIp(event.getClientIp())
                .userAgent(event.getUserAgent())
                .deviceInfo(event.getDeviceId())
                .eventTime(event.getOperateTime() != null ? event.getOperateTime() : LocalDateTime.now())
                .messageTimestamp(timestamp != null ? timestamp : System.currentTimeMillis())
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 敏感信息脱敏（简化版）
     */
    private void sanitizeSensitiveData(PaymentEventDocument document) {
        // 对于简化的事件模型，暂时不需要脱敏处理
        // 可以根据实际需要后续添加
    }


    /**
     * 生成文档ID
     */
    private String generateDocumentId(String paymentId, String eventType, String traceId) {
        return String.format("payment_%s_%s_%s_%d", paymentId, eventType, traceId, System.currentTimeMillis());
    }
}
