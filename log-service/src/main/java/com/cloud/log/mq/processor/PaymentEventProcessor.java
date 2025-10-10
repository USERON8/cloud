package com.cloud.log.messaging.processor;

import com.cloud.common.domain.event.payment.PaymentChangeEvent;
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
            if (paymentEventService.existsByPaymentIdAndEventType(String.valueOf(event.getPaymentId()), eventType, traceId)) {
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
                .paymentId(String.valueOf(event.getPaymentId()))
                .orderId(String.valueOf(event.getOrderId()))
                .userId(event.getUserId())
                .eventType(eventType)
                .tag(tag)
                .traceId(traceId)
                .paymentMethod(convertPaymentMethodToInteger(event.getPaymentMethod()))
                .paymentAmount(event.getAmount())
                .oldPaymentStatus(convertPaymentStatusToInteger(event.getBeforeStatus()))
                .paymentStatus(convertPaymentStatusToInteger(event.getAfterStatus()))
                .thirdPartyTxnId(null) // PaymentChangeEvent没有这个字段
                .productDescription(null) // PaymentChangeEvent没有这个字段
                .remark(event.getRemark())
                .operatorId(null) // PaymentChangeEvent没有这个字段，使用operator字段
                .operatorName(event.getOperator())
                .paymentIp(null) // PaymentChangeEvent没有这个字段
                .userAgent(null) // PaymentChangeEvent没有这个字段
                .deviceInfo(null) // PaymentChangeEvent没有这个字段
                .eventTime(event.getChangeTime() != null ? event.getChangeTime() : LocalDateTime.now())
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
    private String generateDocumentId(Long paymentId, String eventType, String traceId) {
        return String.format("payment_%s_%s_%s_%d", paymentId, eventType, traceId, System.currentTimeMillis());
    }

    /**
     * 转换支付方式字符串为整数
     */
    private Integer convertPaymentMethodToInteger(String paymentMethod) {
        if (paymentMethod == null) {
            return 0; // 默认值
        }

        // 根据支付方式字符串转换为对应的整数值
        switch (paymentMethod.toUpperCase()) {
            case "ALIPAY":
            case "支付宝":
                return 1;
            case "WECHAT":
            case "WECHAT_PAY":
            case "微信":
            case "微信支付":
                return 2;
            case "BANK_CARD":
            case "银行卡":
                return 3;
            case "REFUND":
            case "退款":
                return 99;
            default:
                return 0; // 未知支付方式
        }
    }

    /**
     * 转换支付状态字符串为整数
     */
    private Integer convertPaymentStatusToInteger(String status) {
        if (status == null) {
            return 0; // 默认值
        }

        // 根据支付状态字符串转换为对应的整数值
        switch (status.toUpperCase()) {
            case "PENDING":
            case "待支付":
                return 1;
            case "PROCESSING":
            case "支付中":
                return 2;
            case "SUCCESS":
            case "PAID":
            case "支付成功":
                return 3;
            case "FAILED":
            case "支付失败":
                return 4;
            case "REFUNDED":
            case "REFUND_PENDING":
            case "已退款":
                return 5;
            default:
                // 尝试直接解析为数字
                try {
                    return Integer.parseInt(status);
                } catch (NumberFormatException e) {
                    return 0; // 未知状态
                }
        }
    }
}
