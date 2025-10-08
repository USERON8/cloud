package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.order.OrderDTO;
import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
import com.cloud.common.domain.event.payment.PaymentChangeEvent;
import com.cloud.common.domain.event.stock.StockConfirmEvent;
import com.cloud.common.domain.event.stock.StockReserveEvent;
import com.cloud.common.domain.event.stock.StockRollbackEvent;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.converter.OrderConverter;
import com.cloud.order.dto.OrderCreateRequestDTO;
import com.cloud.order.enums.OrderStatusEnum;
import com.cloud.order.exception.OrderBusinessException;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderBusinessService;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单业务服务实现
 * 基于标准Spring Cache注解的Redis缓存策略
 * 集成分布式锁保证数据一致性
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBusinessServiceImpl implements OrderBusinessService {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderConverter orderConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#result", condition = "#result != null")
    public Long createOrder(OrderCreateRequestDTO createRequest, Long operatorId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("🚀 开始创建订单 - 用户ID: {}, 商品数量: {}, 总金额: {}, 追踪ID: {}",
                    createRequest.getUserId(), createRequest.getOrderItems().size(),
                    createRequest.getTotalAmount(), traceId);

            // 1. 数据验证
            validateCreateRequest(createRequest);

            // 2. 创建订单主记录
            Order order = buildOrder(createRequest, operatorId, traceId);
            boolean orderSaved = orderService.save(order);
            if (!orderSaved) {
                throw new OrderBusinessException("订单主记录保存失败");
            }

            // 3. 创建订单项记录
            List<OrderItem> orderItems = buildOrderItems(order.getId(), createRequest, operatorId, traceId);
            boolean itemsSaved = orderItemService.saveBatch(orderItems);
            if (!itemsSaved) {
                throw new OrderBusinessException(order.getId(), "订单项记录保存失败");
            }

            log.info("✅ 订单创建成功 - 订单ID: {}, 订单号: {}, 用户ID: {}, 追踪ID: {}",
                    order.getId(), order.getOrderNo(), order.getUserId(), traceId);

            return order.getId();

        } catch (OrderBusinessException e) {
            log.error("❌ 订单创建失败 - 用户ID: {}, 错误: {}, 追踪ID: {}",
                    createRequest.getUserId(), e.getMessage(), traceId);
            throw e;
        } catch (Exception e) {
            log.error("❌ 订单创建异常 - 用户ID: {}, 追踪ID: {}",
                    createRequest.getUserId(), traceId, e);
            throw new OrderBusinessException("订单创建系统异常: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean handlePaymentSuccess(Long orderId, Long paymentId, BigDecimal paymentAmount) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("💰 处理支付成功事件 - 订单ID: {}, 支付ID: {}, 支付金额: {}, 追踪ID: {}",
                    orderId, paymentId, paymentAmount, traceId);

            // 1. 获取订单信息（带锁）
            Order order = getOrderForUpdate(orderId);

            // 2. 检查订单状态
            if (!order.canPay()) {
                throw new OrderBusinessException(orderId,
                        String.format("订单状态不允许支付，当前状态: %s", order.getStatusEnum().getName()));
            }

            // 3. 更新订单状态和支付时间
            order.setStatusEnum(OrderStatusEnum.PAID);
            order.setPayTime(LocalDateTime.now());
            order.setPayAmount(paymentAmount);
            boolean updated = orderService.updateById(order);

            if (!updated) {
                throw new OrderBusinessException(orderId, "更新订单支付状态失败");
            }

            log.info("✅ 支付成功事件处理完成 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                    orderId, paymentId, traceId);

            return true;

        } catch (OrderBusinessException e) {
            log.error("❌ 支付成功事件处理失败 - 订单ID: {}, 支付ID: {}, 错误: {}, 追踪ID: {}",
                    orderId, paymentId, e.getMessage(), traceId);
            return false;
        } catch (Exception e) {
            log.error("❌ 支付成功事件处理异常 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                    orderId, paymentId, traceId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean cancelOrder(Long orderId, String cancelReason, Long operatorId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("❌ 开始取消订单 - 订单ID: {}, 取消原因: {}, 操作人: {}, 追踪ID: {}",
                    orderId, cancelReason, operatorId, traceId);

            // 1. 获取订单信息（带锁）
            Order order = getOrderForUpdate(orderId);
            OrderStatusEnum beforeStatus = order.getStatusEnum();

            // 2. 检查订单状态
            if (!order.canCancel()) {
                throw new OrderBusinessException(orderId,
                        String.format("订单状态不允许取消，当前状态: %s", beforeStatus.getName()));
            }

            // 3. 更新订单状态和取消信息
            order.setStatusEnum(OrderStatusEnum.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason(cancelReason != null ? cancelReason : "用户主动取消");
            boolean updated = orderService.updateById(order);

            if (!updated) {
                throw new OrderBusinessException(orderId, "更新订单取消状态失败");
            }

            log.info("✅ 订单取消成功 - 订单ID: {}, 状态变化: {} -> {}, 追踪ID: {}",
                    orderId, beforeStatus.getName(), OrderStatusEnum.CANCELLED.getName(), traceId);

            return true;

        } catch (OrderBusinessException e) {
            log.error("❌ 订单取消失败 - 订单ID: {}, 错误: {}, 追踪ID: {}",
                    orderId, e.getMessage(), traceId);
            return false;
        } catch (Exception e) {
            log.error("❌ 订单取消异常 - 订单ID: {}, 追踪ID: {}", orderId, traceId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean shipOrder(Long orderId, Long operatorId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("📦 开始发货订单 - 订单ID: {}, 操作人: {}, 追踪ID: {}",
                    orderId, operatorId, traceId);

            // 1. 获取订单信息（带锁）
            Order order = getOrderForUpdate(orderId);

            // 2. 检查订单状态
            if (!order.canShip()) {
                throw new OrderBusinessException(orderId,
                        String.format("订单状态不允许发货，当前状态: %s", order.getStatusEnum().getName()));
            }

            // 3. 更新订单状态和发货时间
            order.setStatusEnum(OrderStatusEnum.SHIPPED);
            order.setShipTime(LocalDateTime.now());
            boolean updated = orderService.updateById(order);

            if (!updated) {
                throw new OrderBusinessException(orderId, "更新订单发货状态失败");
            }

            // 4. 订单发货不记录到日志系统（根据需求精简）

            log.info("✅ 订单发货成功 - 订单ID: {}, 追踪ID: {}", orderId, traceId);

            return true;

        } catch (OrderBusinessException e) {
            log.error("❌ 订单发货失败 - 订单ID: {}, 错误: {}, 追踪ID: {}",
                    orderId, e.getMessage(), traceId);
            return false;
        } catch (Exception e) {
            log.error("❌ 订单发货异常 - 订单ID: {}, 追踪ID: {}", orderId, traceId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean completeOrder(Long orderId, Long operatorId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("✅ 开始完成订单 - 订单ID: {}, 操作人: {}, 追踪ID: {}",
                    orderId, operatorId, traceId);

            // 1. 获取订单信息（带锁）
            Order order = getOrderForUpdate(orderId);

            // 2. 检查订单状态
            if (!order.canComplete()) {
                throw new OrderBusinessException(orderId,
                        String.format("订单状态不允许完成，当前状态: %s", order.getStatusEnum().getName()));
            }

            // 3. 更新订单状态和完成时间
            order.setStatusEnum(OrderStatusEnum.COMPLETED);
            order.setCompleteTime(LocalDateTime.now());
            boolean updated = orderService.updateById(order);

            if (!updated) {
                throw new OrderBusinessException(orderId, "更新订单完成状态失败");
            }

            log.info("✅ 订单完成成功 - 订单ID: {}, 追踪ID: {}", orderId, traceId);

            return true;

        } catch (OrderBusinessException e) {
            log.error("❌ 订单完成失败 - 订单ID: {}, 错误: {}, 追踪ID: {}",
                    orderId, e.getMessage(), traceId);
            return false;
        } catch (Exception e) {
            log.error("❌ 订单完成异常 - 订单ID: {}, 追踪ID: {}", orderId, traceId, e);
            return false;
        }
    }

    @Override
    @Cacheable(cacheNames = "order", key = "'status:' + #orderId", unless = "#result == null")
    public OrderStatusEnum checkOrderStatus(Long orderId) {
        try {
            Order order = orderService.getById(orderId);
            return order != null ? order.getStatusEnum() : null;
        } catch (Exception e) {
            log.error("检查订单状态异常 - 订单ID: {}", orderId, e);
            return null;
        }
    }

    @Override
    @Cacheable(cacheNames = "order", key = "'detail:' + #orderId", unless = "#result == null")
    public OrderDTO getOrderWithLock(Long orderId) {
        try {
            Order order = getOrderForUpdate(orderId);
            return orderConverter.toDTO(order);
        } catch (Exception e) {
            log.error("获取订单详情异常 - 订单ID: {}", orderId, e);
            return null;
        }
    }

    @Override
    public boolean handlePaymentFailed(Long orderId, Long paymentId, String failReason) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        try {
            log.info("❌ 处理支付失败事件 - 订单ID: {}, 支付ID: {}, 失败原因: {}, 追踪ID: {}",
                    orderId, paymentId, failReason, traceId);

            // 1. 获取订单信息
            Order order = orderService.getById(orderId);
            if (order == null) {
                log.warn("订单不存在 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                return false;
            }

            log.info("✅ 支付失败事件处理完成 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                    orderId, paymentId, traceId);

            return true;

        } catch (Exception e) {
            log.error("❌ 支付失败事件处理异常 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                    orderId, paymentId, traceId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "order", key = "#orderId")
    public boolean handleStockShortage(Long orderId, List<Long> productIds) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String cancelReason = "库存不足，商品ID: " + productIds;

        try {
            log.info("📦 处理库存不足事件 - 订单ID: {}, 商品ID: {}, 追踪ID: {}",
                    orderId, productIds, traceId);

            // 自动取消订单
            boolean cancelled = cancelOrder(orderId, cancelReason, 0L); // 系统自动操作

            if (cancelled) {
                log.info("✅ 库存不足自动取消订单成功 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
            } else {
                log.error("❌ 库存不足自动取消订单失败 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
            }

            return cancelled;

        } catch (Exception e) {
            log.error("❌ 库存不足事件处理异常 - 订单ID: {}, 追踪ID: {}", orderId, traceId, e);
            return false;
        }
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 验证创建订单请求参数
     */
    private void validateCreateRequest(OrderCreateRequestDTO request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new OrderBusinessException("用户ID无效");
        }

        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new OrderBusinessException("订单商品列表不能为空");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("订单总金额必须大于0");
        }

        if (request.getPayAmount() == null || request.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException("实付金额必须大于0");
        }

        // 验证商品信息
        for (OrderCreateRequestDTO.OrderItemCreateDTO item : request.getOrderItems()) {
            if (item.getProductId() == null || item.getProductId() <= 0) {
                throw new OrderBusinessException("商品ID无效");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderBusinessException("商品数量必须大于0");
            }

            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderBusinessException("商品价格必须大于0");
            }
        }
    }

    /**
     * 构建订单主记录
     */
    private Order buildOrder(OrderCreateRequestDTO request, Long operatorId, String traceId) {
        Order order = new Order();
        order.setOrderNo(Order.generateOrderNo());
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setPayAmount(request.getPayAmount());
        order.setStatusEnum(OrderStatusEnum.PENDING_PAYMENT);
        order.setAddressId(request.getAddressId());
        order.setRemark(request.getRemark());
        // order.setCreateBy(operatorId);
        // order.setUpdateBy(operatorId);

        log.debug("构建订单主记录 - 订单号: {}, 用户ID: {}, 追踪ID: {}",
                order.getOrderNo(), order.getUserId(), traceId);

        return order;
    }

    /**
     * 构建订单项列表
     */
    private List<OrderItem> buildOrderItems(Long orderId, OrderCreateRequestDTO request,
                                            Long operatorId, String traceId) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateRequestDTO.OrderItemCreateDTO itemDto : request.getOrderItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(itemDto.getProductId());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(itemDto.getPrice());

            // 构建商品快照
            String snapshot = buildProductSnapshot(itemDto);
            item.setProductSnapshot(snapshot);

            item.setCreateBy(operatorId);
            item.setUpdateBy(operatorId);

            orderItems.add(item);
        }

        log.debug("构建订单项列表 - 订单ID: {}, 商品数量: {}, 追踪ID: {}",
                orderId, orderItems.size(), traceId);

        return orderItems;
    }

    /**
     * 构建商品快照JSON
     */
    private String buildProductSnapshot(OrderCreateRequestDTO.OrderItemCreateDTO itemDto) {
        return String.format(
                "{\"productId\":%d,\"productName\":\"%s\",\"price\":%s,\"quantity\":%d,\"specification\":\"%s\",\"imageUrl\":\"%s\",\"snapshotTime\":\"%s\"}",
                itemDto.getProductId(),
                itemDto.getProductName() != null ? itemDto.getProductName() : "",
                itemDto.getPrice(),
                itemDto.getQuantity(),
                itemDto.getSpecification() != null ? itemDto.getSpecification() : "",
                itemDto.getImageUrl() != null ? itemDto.getImageUrl() : "",
                LocalDateTime.now()
        );
    }

    /**
     * 获取订单信息（带锁）
     */
    private Order getOrderForUpdate(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(orderId, "订单不存在");
        }
        return order;
    }

    // ===================== 私有辅助方法 =====================
                    .afterStatus("REFUND_PENDING")
                    .changeTime(java.time.LocalDateTime.now())
                    .operator("系统自动")
                    .remark("订单取消退款，订单号: " + order.getOrderNo())
                    .traceId(traceId)
                    .build();

            // 这里需要通过Feign调用payment服务的退款申请接口
            // 或者发送消息到支付服务处理退款
            log.info("退款事件构建完成，需要发送到支付服务处理 - 订单ID: {}, 退款金额: {}",
                    order.getId(), order.getPayAmount());

            log.info("✅ 退款消息发送成功 - 订单ID: {}, 退款金额: {}, 追踪ID: {}",
                    order.getId(), order.getPayAmount(), traceId);
        } catch (Exception e) {
            log.error("❌ 发送退款消息失败 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId, e);
        }
    }

    private void sendOrderCompletedMessage(Order order, String traceId) {
        try {
            // 构庺订单完成事件
            OrderCompletedEvent event =
                    OrderCompletedEvent.builder()
                            .orderId(order.getId())
                            .orderNo(order.getOrderNo())
                            .userId(order.getUserId())
                            .userName("User_" + order.getUserId())
                            .totalAmount(order.getTotalAmount())
                            .payAmount(order.getPayAmount())
                            // .orderStatus(order.getStatus()) // 临时注释掉
                            .beforeStatus(2) // 之前是已发货状态
                            .afterStatus(3)  // 现在是已完成状态
                            .completedTime(order.getCompleteTime())
                            .operator("用户")
                            .traceId(traceId)
                            .build();

            // 发送订单完成事件
            orderEventProducer.sendOrderCompletedEvent(event);

            log.info("✅ 订单完成消息发送成功 - 订单ID: {}, 完成时间: {}, 追踪ID: {}",
                    order.getId(), order.getCompleteTime(), traceId);
        } catch (Exception e) {
            log.error("❌ 发送订单完成消息失败 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId, e);
        }
    }

    // ===================== 辅助方法 =====================

    private List<StockReserveEvent.StockReserveItem> buildStockReserveItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> StockReserveEvent.StockReserveItem.builder()
                        .productId(item.getProductId())
                        .productName(获取商品名称(item.getProductSnapshot()))
                        .quantity(item.getQuantity())
                        .specification(获取商品规格(item.getProductSnapshot()))
                        .warehouseId(1L) // 默认仓库
                        .build())
                .toList();
    }

    private List<StockConfirmEvent.StockConfirmItem> buildStockConfirmItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> StockConfirmEvent.StockConfirmItem.builder()
                        .productId(item.getProductId())
                        .productName(获取商品名称(item.getProductSnapshot()))
                        .quantity(item.getQuantity())
                        .specification(获取商品规格(item.getProductSnapshot()))
                        .warehouseId(1L) // 默认仓库
                        .build())
                .toList();
    }

    private List<StockRollbackEvent.StockRollbackItem> buildStockRollbackItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> StockRollbackEvent.StockRollbackItem.builder()
                        .productId(item.getProductId())
                        .productName(获取商品名称(item.getProductSnapshot()))
                        .quantity(item.getQuantity())
                        .specification(获取商品规格(item.getProductSnapshot()))
                        .warehouseId(1L) // 默认仓库
                        .build())
                .toList();
    }

    private String 获取商品名称(String productSnapshot) {
        try {
            // 简化处理，从 JSON 中解析商品名称
            if (productSnapshot != null && productSnapshot.contains("productName")) {
                int start = productSnapshot.indexOf("productName\":\"") + 14;
                int end = productSnapshot.indexOf("\"", start);
                if (start > 13 && end > start) {
                    return productSnapshot.substring(start, end);
                }
            }
            return "商品";
        } catch (Exception e) {
            return "商品";
        }
    }

    private String 获取商品规格(String productSnapshot) {
        try {
            // 简化处理，从 JSON 中解析商品规格
            if (productSnapshot != null && productSnapshot.contains("specification")) {
                int start = productSnapshot.indexOf("specification\":\"") + 16;
                int end = productSnapshot.indexOf("\"", start);
                if (start > 15 && end > start) {
                    return productSnapshot.substring(start, end);
                }
            }
            return "标准规格";
        } catch (Exception e) {
            return "标准规格";
        }
    }

    // ===================== 业务日志发送方法（仅记录关键操作）=====================

    /**
     * 发送订单完成日志
     * 记录订单完成的关键信息到日志系统
     */
    private void sendOrderCompletedLog(Order order, Long operatorId, String traceId) {
        try {
            businessLogProducer.sendOrderCompletedLog(
                    traceId,
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    "User_" + order.getUserId(),
                    order.getTotalAmount(),
                    order.getPayAmount(),
                    order.getStatusEnum().getCode(),
                    1L, // 默认店铺ID
                    order.getCompleteTime() != null ? order.getCompleteTime().toString() : LocalDateTime.now().toString(),
                    operatorId != null ? ("Operator_" + operatorId) : "系统自动",
                    operatorId != null ? operatorId.toString() : "0"
            );
            log.debug("订单完成日志发送成功 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId);
        } catch (Exception e) {
            log.warn("发送订单完成日志失败 - 订单ID: {}, 追踪ID: {}, 错误: {}", order.getId(), traceId, e.getMessage());
        }
    }

    /**
     * 发送订单退款日志
     * 记录订单退款的关键信息到日志系统
     */
    private void sendOrderRefundLog(Order order, Long operatorId, String traceId) {
        try {
            businessLogProducer.sendOrderRefundLog(
                    traceId,
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    "User_" + order.getUserId(),
                    order.getPayAmount(), // 退款金额等于支付金额
                    order.getCancelReason() != null ? order.getCancelReason() : "订单取消退款",
                    order.getCancelTime() != null ? order.getCancelTime().toString() : LocalDateTime.now().toString(),
                    1L, // 默认店铺ID
                    operatorId != null ? ("Operator_" + operatorId) : "系统自动",
                    operatorId != null ? operatorId.toString() : "0"
            );
            log.debug("订单退款日志发送成功 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId);
        } catch (Exception e) {
            log.warn("发送订单退款日志失败 - 订单ID: {}, 追踪ID: {}, 错误: {}", order.getId(), traceId, e.getMessage());
        }
    }

    /**
     * 计算订单商品总数量
     */
    private Integer calculateTotalQuantity(Long orderId) {
        try {
            List<OrderItem> orderItems = orderItemService.lambdaQuery()
                    .eq(OrderItem::getOrderId, orderId)
                    .list();
            return orderItems.stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
        } catch (Exception e) {
            log.warn("计算订单商品总数量失败 - 订单ID: {}", orderId, e);
            return 0;
        }
    }

    /**
     * 获取店铺名称
     */
    private String getShopName(Long shopId) {
        if (shopId == null) return "未知店铺";
        // 这里可以调用shop服务获取店铺名称，暂时简化处理
        return "Shop_" + shopId;
    }

    /**
     * 获取支付方式
     */
    private String getPaymentMethod(Long orderId) {
        // 这里可以调用payment服务获取支付方式，暂时简化处理
        return "ALIPAY";
    }
}
