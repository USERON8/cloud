package com.cloud.common.utils;

import com.cloud.common.domain.event.order.OrderChangeEvent;
import com.cloud.common.domain.event.payment.PaymentChangeEvent;
import com.cloud.common.domain.event.product.ProductChangeEvent;
import com.cloud.common.domain.event.stock.StockChangeEvent;
import com.cloud.common.domain.event.user.UserChangeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 事件构建工具类
 * 
 * 提供标准化的事件创建方法，简化事件构建过程
 * 自动填充公共字段：timestamp, traceId
 * 支持metadata的JSON构建
 * 
 * @author CloudDevAgent
 * @since 2025-10-04
 */
@Slf4j
public class EventBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_OPERATOR = "system";

    // ======================== 用户事件构建器 ========================

    /**
     * 构建用户变更事件
     * 
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param operator 操作人
     * @return 用户变更事件
     */
    public static UserChangeEvent buildUserChangeEvent(
            Long userId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator) {
        return UserChangeEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .build();
    }

    /**
     * 构建用户变更事件（带metadata）
     */
    public static UserChangeEvent buildUserChangeEvent(
            Long userId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator,
            Map<String, Object> metadataMap) {
        return UserChangeEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .metadata(toJsonString(metadataMap))
                .build();
    }

    /**
     * 构建用户登录事件
     */
    public static UserChangeEvent buildUserLoginEvent(Long userId, String ip, String device) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ip", ip);
        metadata.put("device", device);
        metadata.put("loginTime", LocalDateTime.now().toString());

        return buildUserChangeEvent(userId, "LOGIN", null, 1, "system", metadata);
    }

    /**
     * 构建用户登出事件
     */
    public static UserChangeEvent buildUserLogoutEvent(Long userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("logoutTime", LocalDateTime.now().toString());

        return buildUserChangeEvent(userId, "LOGOUT", 1, null, "system", metadata);
    }

    // ======================== 订单事件构建器 ========================

    /**
     * 构建订单变更事件
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param totalAmount 订单总额
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param operator 操作人
     * @return 订单变更事件
     */
    public static OrderChangeEvent buildOrderChangeEvent(
            Long orderId,
            Long userId,
            String eventType,
            BigDecimal totalAmount,
            Integer beforeStatus,
            Integer afterStatus,
            String operator) {
        return OrderChangeEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .eventType(eventType)
                .totalAmount(totalAmount)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .build();
    }

    /**
     * 构建订单变更事件（带metadata）
     */
    public static OrderChangeEvent buildOrderChangeEvent(
            Long orderId,
            Long userId,
            String eventType,
            BigDecimal totalAmount,
            Integer beforeStatus,
            Integer afterStatus,
            String operator,
            Map<String, Object> metadataMap) {
        return OrderChangeEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .eventType(eventType)
                .totalAmount(totalAmount)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .metadata(toJsonString(metadataMap))
                .build();
    }

    /**
     * 构建订单创建事件
     */
    public static OrderChangeEvent buildOrderCreatedEvent(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            BigDecimal payAmount,
            Integer itemCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payAmount", payAmount.toString());
        metadata.put("itemCount", itemCount);

        return buildOrderChangeEvent(orderId, userId, "CREATED", totalAmount, null, 0, "system", metadata);
    }

    /**
     * 构建订单支付完成事件
     */
    public static OrderChangeEvent buildOrderPaidEvent(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            Long paymentId,
            String paymentMethod) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", paymentId);
        metadata.put("paymentMethod", paymentMethod);

        return buildOrderChangeEvent(orderId, userId, "PAID", totalAmount, 0, 1, "system", metadata);
    }

    /**
     * 构建订单取消事件
     */
    public static OrderChangeEvent buildOrderCancelledEvent(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            String reason,
            BigDecimal refundAmount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("refundAmount", refundAmount.toString());

        return buildOrderChangeEvent(orderId, userId, "CANCELLED", totalAmount, 1, -1, "system", metadata);
    }

    // ======================== 支付事件构建器 ========================

    /**
     * 构建支付变更事件
     * 
     * @param paymentId 支付ID
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param amount 支付金额
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param operator 操作人
     * @return 支付变更事件
     */
    public static PaymentChangeEvent buildPaymentChangeEvent(
            Long paymentId,
            Long orderId,
            Long userId,
            String eventType,
            BigDecimal amount,
            Integer beforeStatus,
            Integer afterStatus,
            String operator) {
        return PaymentChangeEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .eventType(eventType)
                .amount(amount)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .build();
    }

    /**
     * 构建支付变更事件（带metadata）
     */
    public static PaymentChangeEvent buildPaymentChangeEvent(
            Long paymentId,
            Long orderId,
            Long userId,
            String eventType,
            BigDecimal amount,
            Integer beforeStatus,
            Integer afterStatus,
            String operator,
            Map<String, Object> metadataMap) {
        return PaymentChangeEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .eventType(eventType)
                .amount(amount)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .metadata(toJsonString(metadataMap))
                .build();
    }

    /**
     * 构建支付成功事件
     */
    public static PaymentChangeEvent buildPaymentSuccessEvent(
            Long paymentId,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String paymentMethod,
            String transactionNo) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentMethod", paymentMethod);
        metadata.put("transactionNo", transactionNo);
        metadata.put("paidTime", LocalDateTime.now().toString());

        return buildPaymentChangeEvent(paymentId, orderId, userId, "SUCCESS", amount, 0, 1, "system", metadata);
    }

    /**
     * 构建退款事件
     */
    public static PaymentChangeEvent buildPaymentRefundEvent(
            Long paymentId,
            Long orderId,
            Long userId,
            BigDecimal refundAmount,
            String refundReason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("refundAmount", refundAmount.toString());
        metadata.put("refundReason", refundReason);
        metadata.put("refundTime", LocalDateTime.now().toString());

        return buildPaymentChangeEvent(paymentId, orderId, userId, "REFUND", refundAmount, 1, 2, "system", metadata);
    }

    // ======================== 商品事件构建器 ========================

    /**
     * 构建商品变更事件
     * 
     * @param productId 商品ID
     * @param shopId 店铺ID
     * @param categoryId 分类ID
     * @param eventType 事件类型
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param operator 操作人
     * @return 商品变更事件
     */
    public static ProductChangeEvent buildProductChangeEvent(
            Long productId,
            Long shopId,
            Long categoryId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator) {
        return ProductChangeEvent.builder()
                .productId(productId)
                .shopId(shopId)
                .categoryId(categoryId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .build();
    }

    /**
     * 构建商品变更事件（带metadata）
     */
    public static ProductChangeEvent buildProductChangeEvent(
            Long productId,
            Long shopId,
            Long categoryId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator,
            Map<String, Object> metadataMap) {
        return ProductChangeEvent.builder()
                .productId(productId)
                .shopId(shopId)
                .categoryId(categoryId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .metadata(toJsonString(metadataMap))
                .build();
    }

    /**
     * 构建商品创建事件
     */
    public static ProductChangeEvent buildProductCreatedEvent(
            Long productId,
            Long shopId,
            Long categoryId,
            String productName,
            BigDecimal price,
            Integer stock) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productName", productName);
        metadata.put("price", price.toString());
        metadata.put("stock", stock);

        return buildProductChangeEvent(productId, shopId, categoryId, "CREATED", null, 1, "system", metadata);
    }

    /**
     * 构建商品库存变更事件
     */
    public static ProductChangeEvent buildProductStockChangedEvent(
            Long productId,
            Long shopId,
            Long categoryId,
            Integer beforeStock,
            Integer changeStock,
            Integer afterStock) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("beforeStock", beforeStock);
        metadata.put("changeStock", changeStock);
        metadata.put("afterStock", afterStock);

        return buildProductChangeEvent(productId, shopId, categoryId, "STOCK_CHANGED", 1, 1, "system", metadata);
    }

    // ======================== 库存事件构建器 ========================

    /**
     * 构建库存变更事件
     * 
     * @param stockId 库存ID
     * @param productId 商品ID
     * @param orderId 订单ID（可选）
     * @param eventType 事件类型
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param operator 操作人
     * @return 库存变更事件
     */
    public static StockChangeEvent buildStockChangeEvent(
            Long stockId,
            Long productId,
            Long orderId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator) {
        return StockChangeEvent.builder()
                .stockId(stockId)
                .productId(productId)
                .orderId(orderId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .build();
    }

    /**
     * 构建库存变更事件（带metadata）
     */
    public static StockChangeEvent buildStockChangeEvent(
            Long stockId,
            Long productId,
            Long orderId,
            String eventType,
            Integer beforeStatus,
            Integer afterStatus,
            String operator,
            Map<String, Object> metadataMap) {
        return StockChangeEvent.builder()
                .stockId(stockId)
                .productId(productId)
                .orderId(orderId)
                .eventType(eventType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .operator(operator != null ? operator : DEFAULT_OPERATOR)
                .metadata(toJsonString(metadataMap))
                .build();
    }

    /**
     * 构建库存预占事件
     */
    public static StockChangeEvent buildStockReservedEvent(
            Long stockId,
            Long productId,
            Long orderId,
            Integer reserveQuantity,
            Integer remainingStock) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reserveQuantity", reserveQuantity);
        metadata.put("remainingStock", remainingStock);

        return buildStockChangeEvent(stockId, productId, orderId, "RESERVED", 1, 2, "system", metadata);
    }

    /**
     * 构建库存确认扣减事件
     */
    public static StockChangeEvent buildStockConfirmedEvent(
            Long stockId,
            Long productId,
            Long orderId,
            Integer confirmQuantity,
            Integer beforeStock,
            Integer afterStock) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("confirmQuantity", confirmQuantity);
        metadata.put("beforeStock", beforeStock);
        metadata.put("afterStock", afterStock);

        return buildStockChangeEvent(stockId, productId, orderId, "CONFIRMED", 2, 1, "system", metadata);
    }

    /**
     * 构建库存回滚事件
     */
    public static StockChangeEvent buildStockRollbackEvent(
            Long stockId,
            Long productId,
            Long orderId,
            Integer rollbackQuantity,
            String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rollbackQuantity", rollbackQuantity);
        metadata.put("reason", reason);

        return buildStockChangeEvent(stockId, productId, orderId, "ROLLBACK", 2, 1, "system", metadata);
    }

    // ======================== 工具方法 ========================

    /**
     * 生成追踪ID
     * 格式: TRACE-{timestamp}-{uuid前8位}
     */
    public static String generateTraceId() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "TRACE-" + System.currentTimeMillis() + "-" + uuid;
    }

    /**
     * 将Map转换为JSON字符串
     */
    private static String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("❌ 转换metadata为JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析JSON字符串为Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(metadata, Map.class);
        } catch (JsonProcessingException e) {
            log.error("❌ 解析metadata JSON失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}

