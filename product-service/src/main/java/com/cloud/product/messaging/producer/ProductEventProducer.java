package com.cloud.product.messaging.producer;

import com.cloud.common.domain.event.MerchantChangeEvent;
import com.cloud.common.domain.event.product.ProductChangeEvent;
import com.cloud.common.exception.MessageSendException;
import com.cloud.common.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品事件生产者
 * 负责发送商品变更和商家变更事件到RocketMQ
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class ProductEventProducer {

    private static final String PRODUCT_BINDING_NAME = "product-producer-out-0";
    private static final String MERCHANT_BINDING_NAME = "merchant-producer-out-0";
    private final StreamBridge streamBridge;

    /**
     * 发送商品创建事件
     */
    public void sendProductCreatedEvent(ProductChangeEvent event) {
        sendProductEvent(event, "PRODUCT_CREATED", "product-created");
    }

    /**
     * 发送商品更新事件
     */
    public void sendProductUpdatedEvent(ProductChangeEvent event) {
        sendProductEvent(event, "PRODUCT_UPDATED", "product-updated");
    }

    /**
     * 发送商品删除事件
     */
    public void sendProductDeletedEvent(ProductChangeEvent event) {
        sendProductEvent(event, "PRODUCT_DELETED", "product-deleted");
    }

    /**
     * 发送商品上架事件
     */
    public void sendProductOnSaleEvent(ProductChangeEvent event) {
        sendProductEvent(event, "PRODUCT_ON_SALE", "product-on-sale");
    }

    /**
     * 发送商品下架事件
     */
    public void sendProductOffSaleEvent(ProductChangeEvent event) {
        sendProductEvent(event, "PRODUCT_OFF_SALE", "product-off-sale");
    }

    /**
     * 发送商家创建事件
     */
    public void sendMerchantCreatedEvent(MerchantChangeEvent event) {
        sendMerchantEvent(event, "MERCHANT_CREATED", "merchant-created");
    }

    /**
     * 发送商家更新事件
     */
    public void sendMerchantUpdatedEvent(MerchantChangeEvent event) {
        sendMerchantEvent(event, "MERCHANT_UPDATED", "merchant-updated");
    }

    /**
     * 发送商家状态变更事件
     */
    public void sendMerchantStatusChangedEvent(MerchantChangeEvent event) {
        sendMerchantEvent(event, "MERCHANT_STATUS_CHANGED", "merchant-status-changed");
    }

    /**
     * 统一发送商品事件的内部方法
     * 按照官方示例标准实现，使用GenericMessage和MessageConst
     */
    private void sendProductEvent(ProductChangeEvent event, String changeType, String tag) {
        try {
            // 按照官方示例构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "PRODUCT_" + event.getProductId());
            headers.put("eventType", changeType);
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息（官方标准方式）
            Message<ProductChangeEvent> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("product-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(PRODUCT_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 商品事件发送成功 - 事件类型: {}, 商品ID: {}, Tag: {}, TraceId: {}",
                        changeType, event.getProductId(), tag, traceId);
            } else {
                log.error("❌ 商品事件发送失败 - 事件类型: {}, 商品ID: {}, TraceId: {}",
                        changeType, event.getProductId(), traceId);
                throw new MessageSendException("商品事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送商品事件时发生异常 - 事件类型: {}, 商品ID: {}, 错误: {}",
                    changeType, event.getProductId(), e.getMessage(), e);
            throw new MessageSendException("发送商品事件异常", e);
        }
    }

    /**
     * 统一发送商家事件的内部方法
     */
    private void sendMerchantEvent(MerchantChangeEvent event, String changeType, String tag) {
        try {
            // 按照官方示例构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "MERCHANT_" + event.getMerchantId());
            headers.put("eventType", changeType);
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息（官方标准方式）
            Message<MerchantChangeEvent> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("merchant-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(MERCHANT_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 商家事件发送成功 - 事件类型: {}, 商家ID: {}, Tag: {}, TraceId: {}",
                        changeType, event.getMerchantId(), tag, traceId);
            } else {
                log.error("❌ 商家事件发送失败 - 事件类型: {}, 商家ID: {}, TraceId: {}",
                        changeType, event.getMerchantId(), traceId);
                throw new MessageSendException("商家事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送商家事件时发生异常 - 事件类型: {}, 商家ID: {}, 错误: {}",
                    changeType, event.getMerchantId(), e.getMessage(), e);
            throw new MessageSendException("发送商家事件异常", e);
        }
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
