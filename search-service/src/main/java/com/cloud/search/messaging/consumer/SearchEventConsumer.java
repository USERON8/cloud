package com.cloud.search.messaging.consumer;

import com.cloud.common.domain.event.product.CategorySearchEvent;
import com.cloud.common.domain.event.product.ProductSearchEvent;
import com.cloud.common.domain.event.product.ShopSearchEvent;
import com.cloud.common.utils.MessageUtils;
import com.cloud.search.service.CategorySearchService;
import com.cloud.search.service.ProductSearchService;
import com.cloud.search.service.ShopSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 搜索事件消费者
 * 消费商品、店铺、分类变更事件并同步到Elasticsearch
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SearchEventConsumer {

    private final ProductSearchService productSearchService;
    private final ShopSearchService shopSearchService;
    private final CategorySearchService categorySearchService;

    /**
     * 搜索事件消费者函数
     * 对应绑定名称: searchConsumer-in-0
     */
    @Bean
    public Consumer<Message<Object>> searchConsumer() {
        return message -> {
            try {
                Object payload = message.getPayload();
                String eventType = (String) message.getHeaders().get("eventType");
                String traceId = (String) message.getHeaders().get("traceId");
                String tag = (String) message.getHeaders().get("rocketmq_TAGS");

                log.info("📥 接收到搜索事件 - 事件类型: {}, Tag: {}, 追踪ID: {}",
                        eventType, tag, traceId);

                // 根据Tag分发到不同的处理器
                switch (tag) {
                    case "product-search":
                        if (payload instanceof ProductSearchEvent) {
                            handleProductSearchEvent((ProductSearchEvent) payload, eventType, traceId);
                        }
                        break;
                    case "shop-search":
                        if (payload instanceof ShopSearchEvent) {
                            handleShopSearchEvent((ShopSearchEvent) payload, eventType, traceId);
                        }
                        break;
                    case "category-search":
                        if (payload instanceof CategorySearchEvent) {
                            handleCategorySearchEvent((CategorySearchEvent) payload, eventType, traceId);
                        }
                        break;
                    default:
                        log.warn("⚠️ 未知的搜索事件Tag: {}", tag);
                }

            } catch (Exception e) {
                log.error("❌ 处理搜索事件时发生异常: {}", e.getMessage(), e);
                // 这里可以根据需要决定是否重试或发送到死信队列
            }
        };
    }

    /**
     * 处理商品搜索事件
     */
    private void handleProductSearchEvent(ProductSearchEvent event, String eventType, String traceId) {
        try {
            // 幂等性检查
            if (productSearchService.isEventProcessed(traceId)) {
                log.warn("⚠️ 商品搜索事件已处理，跳过重复处理 - 商品ID: {}, 事件类型: {}, TraceId: {}",
                        event.getProductId(), eventType, traceId);
                return;
            }

            // 记录消息接收日志
            MessageUtils.logMessageReceive("product-search-events", event, traceId);

            // 根据事件类型处理
            switch (eventType) {
                case "PRODUCT_CREATED":
                case "PRODUCT_UPDATED":
                    productSearchService.saveOrUpdateProduct(event);
                    break;
                case "PRODUCT_DELETED":
                    productSearchService.deleteProduct(event.getProductId());
                    break;
                case "PRODUCT_STATUS_CHANGED":
                    productSearchService.updateProductStatus(event.getProductId(), event.getStatus());
                    break;
                default:
                    log.warn("⚠️ 未知的商品搜索事件类型: {}", eventType);
                    return;
            }

            // 标记事件已处理
            productSearchService.markEventProcessed(traceId);

            log.info("✅ 商品搜索事件处理成功 - 商品ID: {}, 事件类型: {}, TraceId: {}",
                    event.getProductId(), eventType, traceId);

        } catch (Exception e) {
            log.error("❌ 处理商品搜索事件失败 - 商品ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    event.getProductId(), eventType, traceId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理店铺搜索事件
     */
    private void handleShopSearchEvent(ShopSearchEvent event, String eventType, String traceId) {
        try {
            // 幂等性检查
            if (shopSearchService.isEventProcessed(traceId)) {
                log.warn("⚠️ 店铺搜索事件已处理，跳过重复处理 - 店铺ID: {}, 事件类型: {}, TraceId: {}",
                        event.getShopId(), eventType, traceId);
                return;
            }

            // 记录消息接收日志
            MessageUtils.logMessageReceive("shop-search-events", event, traceId);

            // 根据事件类型处理
            switch (eventType) {
                case "SHOP_CREATED":
                case "SHOP_UPDATED":
                    shopSearchService.saveOrUpdateShop(event);
                    break;
                case "SHOP_DELETED":
                    shopSearchService.deleteShop(event.getShopId());
                    break;
                case "SHOP_STATUS_CHANGED":
                    shopSearchService.updateShopStatus(event.getShopId(), event.getStatus());
                    break;
                default:
                    log.warn("⚠️ 未知的店铺搜索事件类型: {}", eventType);
                    return;
            }

            // 标记事件已处理
            shopSearchService.markEventProcessed(traceId);

            log.info("✅ 店铺搜索事件处理成功 - 店铺ID: {}, 事件类型: {}, TraceId: {}",
                    event.getShopId(), eventType, traceId);

        } catch (Exception e) {
            log.error("❌ 处理店铺搜索事件失败 - 店铺ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    event.getShopId(), eventType, traceId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理分类搜索事件
     */
    private void handleCategorySearchEvent(CategorySearchEvent event, String eventType, String traceId) {
        try {
            // 幂等性检查
            if (categorySearchService.isEventProcessed(traceId)) {
                log.warn("⚠️ 分类搜索事件已处理，跳过重复处理 - 分类ID: {}, 事件类型: {}, TraceId: {}",
                        event.getCategoryId(), eventType, traceId);
                return;
            }

            // 记录消息接收日志
            MessageUtils.logMessageReceive("category-search-events", event, traceId);

            // 根据事件类型处理
            switch (eventType) {
                case "CATEGORY_CREATED":
                case "CATEGORY_UPDATED":
                    categorySearchService.saveOrUpdateCategory(event);
                    break;
                case "CATEGORY_DELETED":
                    categorySearchService.deleteCategory(event.getCategoryId());
                    break;
                case "CATEGORY_STATUS_CHANGED":
                    categorySearchService.updateCategoryStatus(event.getCategoryId(), event.getStatus());
                    break;
                default:
                    log.warn("⚠️ 未知的分类搜索事件类型: {}", eventType);
                    return;
            }

            // 标记事件已处理
            categorySearchService.markEventProcessed(traceId);

            log.info("✅ 分类搜索事件处理成功 - 分类ID: {}, 事件类型: {}, TraceId: {}",
                    event.getCategoryId(), eventType, traceId);

        } catch (Exception e) {
            log.error("❌ 处理分类搜索事件失败 - 分类ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    event.getCategoryId(), eventType, traceId, e.getMessage(), e);
            throw e;
        }
    }
}
