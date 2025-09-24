package com.cloud.product.messaging;

import com.cloud.common.domain.event.StatisticsEvent;
import com.cloud.common.messaging.AsyncMessageProducer;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 商品服务异步统计生产者
 * 专门用于商品相关的统计数据发送
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncStatisticsProducer {

    private static final String STATISTICS_BINDING_NAME = "statisticsProducer-out-0";
    private final AsyncMessageProducer asyncMessageProducer;

    /**
     * 异步发送商品浏览统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductViewStatisticsAsync(Long productId, Long userId, Long shopId, Long categoryId,
                                                                  String source, String userAgent, String ipAddress) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_VIEW")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .userId(userId)
                .shopId(shopId)
                .categoryId(categoryId)
                .source(source)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_VIEW", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品浏览统计发送成功: productId={}, userId={}", productId, userId))
                .exceptionally(throwable -> {
                    log.warn("商品浏览统计发送失败: productId={}, userId={}, 错误: {}", productId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送商品销售统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductSalesStatisticsAsync(Long productId, Long userId, Long shopId, Long categoryId,
                                                                   Integer quantity, BigDecimal amount, Long orderId) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_SALES")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .userId(userId)
                .shopId(shopId)
                .categoryId(categoryId)
                .quantity(quantity)
                .amount(amount)
                .orderId(orderId)
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_SALES", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品销售统计发送成功: productId={}, quantity={}, amount={}", productId, quantity, amount))
                .exceptionally(throwable -> {
                    log.warn("商品销售统计发送失败: productId={}, 错误: {}", productId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送商品收藏统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductFavoriteStatisticsAsync(Long productId, Long userId, Long shopId, Long categoryId, String action) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_FAVORITE")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .userId(userId)
                .shopId(shopId)
                .categoryId(categoryId)
                .actionType(action) // ADD 或 REMOVE
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_FAVORITE", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品收藏统计发送成功: productId={}, userId={}, action={}", productId, userId, action))
                .exceptionally(throwable -> {
                    log.warn("商品收藏统计发送失败: productId={}, userId={}, 错误: {}", productId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送商品评价统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductReviewStatisticsAsync(Long productId, Long userId, Long shopId, Long categoryId,
                                                                    Integer rating, Long reviewId) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_REVIEW")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .userId(userId)
                .shopId(shopId)
                .categoryId(categoryId)
                .value(rating.longValue())
                .remark("reviewId:" + reviewId.toString())
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_REVIEW", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品评价统计发送成功: productId={}, userId={}, rating={}", productId, userId, rating))
                .exceptionally(throwable -> {
                    log.warn("商品评价统计发送失败: productId={}, userId={}, 错误: {}", productId, userId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送商品库存变更统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductStockChangeStatisticsAsync(Long productId, Long shopId, Long categoryId,
                                                                         Integer beforeStock, Integer afterStock, String changeType, String reason) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_STOCK_CHANGE")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .shopId(shopId)
                .categoryId(categoryId)
                .quantity(afterStock - beforeStock)
                .actionType(changeType) // INCREASE, DECREASE, ADJUST
                .remark("before:" + beforeStock + ",after:" + afterStock + ",reason:" + reason)
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_STOCK_CHANGE", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品库存变更统计发送成功: productId={}, {}→{}", productId, beforeStock, afterStock))
                .exceptionally(throwable -> {
                    log.warn("商品库存变更统计发送失败: productId={}, 错误: {}", productId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送商品价格变更统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendProductPriceChangeStatisticsAsync(Long productId, Long shopId, Long categoryId,
                                                                         BigDecimal beforePrice, BigDecimal afterPrice, String reason) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("PRODUCT_PRICE_CHANGE")
                .businessType("PRODUCT")
                .businessId(productId.toString())
                .shopId(shopId)
                .categoryId(categoryId)
                .amount(afterPrice)
                .remark("before:" + beforePrice + ",after:" + afterPrice + ",reason:" + reason)
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "PRODUCT_PRICE_CHANGE", "PRODUCT", productId.toString())
                .thenRun(() -> log.debug("商品价格变更统计发送成功: productId={}, ¥{}→¥{}", productId, beforePrice, afterPrice))
                .exceptionally(throwable -> {
                    log.warn("商品价格变更统计发送失败: productId={}, 错误: {}", productId, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 异步发送店铺商品统计
     */
    @Async("productStatisticsExecutor")
    public CompletableFuture<Void> sendShopProductStatisticsAsync(Long shopId, String statisticsType, String action, Long productId) {

        StatisticsEvent event = StatisticsEvent.builder()
                .eventId(StringUtils.generateTraceId())
                .eventType("SHOP_PRODUCT_" + statisticsType)
                .businessType("SHOP")
                .businessId(shopId.toString())
                .shopId(shopId)
                .actionType(action) // ADD, REMOVE, UPDATE
                .productId(productId)
                .eventTime(LocalDateTime.now())
                .build();

        return asyncMessageProducer.sendAsync(STATISTICS_BINDING_NAME, event, "SHOP_PRODUCT_" + statisticsType, "SHOP", shopId.toString())
                .thenRun(() -> log.debug("店铺商品统计发送成功: shopId={}, type={}, action={}", shopId, statisticsType, action))
                .exceptionally(throwable -> {
                    log.warn("店铺商品统计发送失败: shopId={}, 错误: {}", shopId, throwable.getMessage());
                    return null;
                });
    }
}
