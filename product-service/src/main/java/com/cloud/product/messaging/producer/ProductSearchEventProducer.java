package com.cloud.product.messaging.producer;

import com.cloud.common.domain.event.product.CategorySearchEvent;
import com.cloud.common.domain.event.product.ProductSearchEvent;
import com.cloud.common.domain.event.product.ShopSearchEvent;
import com.cloud.common.exception.MessageSendException;
import com.cloud.common.utils.MessageUtils;
import com.cloud.common.utils.StringUtils;
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
 * 商品搜索事件生产者
 * 负责发送商品、店铺、分类变更事件到搜索服务
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class ProductSearchEventProducer {

    private static final String SEARCH_BINDING_NAME = "search-producer-out-0";
    private final StreamBridge streamBridge;

    /**
     * 发送商品搜索事件
     */
    public void sendProductSearchEvent(ProductSearchEvent event, String eventType) {
        sendSearchEvent(event, eventType, "product-search", "PRODUCT_" + event.getProductId());
    }

    /**
     * 发送店铺搜索事件
     */
    public void sendShopSearchEvent(ShopSearchEvent event, String eventType) {
        sendSearchEvent(event, eventType, "shop-search", "SHOP_" + event.getShopId());
    }

    /**
     * 发送分类搜索事件
     */
    public void sendCategorySearchEvent(CategorySearchEvent event, String eventType) {
        sendSearchEvent(event, eventType, "category-search", "CATEGORY_" + event.getCategoryId());
    }

    /**
     * 统一发送搜索事件的内部方法
     */
    private void sendSearchEvent(Object event, String eventType, String tag, String key) {
        try {
            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, key);
            headers.put("eventType", eventType);
            headers.put("traceId", StringUtils.generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());
            headers.put("serviceName", "product-service");

            // 构建消息
            Message<Object> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("search-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(SEARCH_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 搜索事件发送成功 - 事件类型: {}, Tag: {}, Key: {}, TraceId: {}",
                        eventType, tag, key, traceId);
            } else {
                log.error("❌ 搜索事件发送失败 - 事件类型: {}, Tag: {}, Key: {}, TraceId: {}",
                        eventType, tag, key, traceId);
                throw new MessageSendException("搜索事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送搜索事件时发生异常 - 事件类型: {}, Tag: {}, 错误: {}",
                    eventType, tag, e.getMessage(), e);
            throw new MessageSendException("发送搜索事件异常", e);
        }
    }
}
