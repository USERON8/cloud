package com.cloud.order.service.impl;

import com.cloud.common.domain.event.OrderCompletedEvent;
import com.cloud.common.domain.event.OrderCreatedEvent;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.order.dto.SimpleOrderCreateDTO;
import com.cloud.order.exception.OrderServiceException;
import com.cloud.order.exception.OrderStatusException;
import com.cloud.order.messaging.producer.LogCollectionProducer;
import com.cloud.order.messaging.producer.OrderEventProducer;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.SimpleOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * 简化订单服务实现类
 * 专门处理单商品订单的业务逻辑，集成事件发布机制
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleOrderServiceImpl implements SimpleOrderService {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderEventProducer orderEventProducer;
    private final LogCollectionProducer logCollectionProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSimpleOrder(SimpleOrderCreateDTO orderCreateDTO, String currentUserId) {
        try {
            log.info("🚀 开始创建单商品订单 - 用户: {}, 商品ID: {}, 数量: {}",
                    currentUserId, orderCreateDTO.getProductId(), orderCreateDTO.getQuantity());

            // 1. 创建订单主记录
            Order order = new Order();
            order.setUserId(Long.valueOf(currentUserId));
            order.setTotalAmount(orderCreateDTO.getTotalAmount());
            order.setPayAmount(orderCreateDTO.getTotalAmount());
            order.setStatus(0); // 待支付状态
            order.setAddressId(orderCreateDTO.getAddressId() != null ? orderCreateDTO.getAddressId() : 1001L);

            boolean orderSaved = orderService.save(order);
            if (!orderSaved) {
                throw new OrderServiceException("创建订单主记录失败");
            }

            // 2. 创建订单项记录
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(orderCreateDTO.getProductId());
            orderItem.setQuantity(orderCreateDTO.getQuantity());
            orderItem.setPrice(orderCreateDTO.getProductPrice());
            orderItem.setCreateBy(Long.valueOf(currentUserId));

            // 创建商品快照
            String productSnapshot = String.format(
                    "{\"productId\":%d,\"productName\":\"%s\",\"price\":%s,\"quantity\":%d,\"createTime\":\"%s\"}",
                    orderCreateDTO.getProductId(),
                    orderCreateDTO.getProductName() != null ? orderCreateDTO.getProductName() : "商品",
                    orderCreateDTO.getProductPrice(),
                    orderCreateDTO.getQuantity(),
                    LocalDateTime.now()
            );
            orderItem.setProductSnapshot(productSnapshot);

            boolean itemSaved = orderItemService.save(orderItem);
            if (!itemSaved) {
                throw new OrderServiceException("创建订单项记录失败");
            }

            // 3. 发布订单创建事件
            publishOrderCreatedEvent(order, orderItem, currentUserId);

            // 4. 发送订单创建日志
            try {
                logCollectionProducer.sendOrderOperationLog(
                        order.getId(),
                        "ORDER_" + order.getId(),
                        order.getUserId(),
                        "CREATE",
                        order.getTotalAmount(),
                        currentUserId
                );
            } catch (Exception e) {
                log.warn("发送简单订单创建日志失败，订单ID：{}", order.getId(), e);
            }

            log.info("✅ 单商品订单创建成功 - 订单ID: {}, 用户: {}, 商品: {}",
                    order.getId(), currentUserId, orderCreateDTO.getProductId());

            return order.getId();

        } catch (Exception e) {
            log.error("❌ 创建单商品订单失败 - 用户: {}, 商品: {}, 错误: {}",
                    currentUserId, orderCreateDTO.getProductId(), e.getMessage(), e);
            throw new OrderServiceException("创建单商品订单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getOrderStatus(Long orderId) {
        try {
            Order order = orderService.getById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            String statusDesc = switch (order.getStatus()) {
                case 0 -> "待支付 - 订单已创建，等待支付";
                case 1 -> "已支付 - 支付成功，准备发货";
                case 2 -> "已发货 - 商品已发出，等待收货";
                case 3 -> "已完成 - 订单已完成";
                case -1 -> "已取消 - 订单已取消";
                default -> "未知状态";
            };

            return String.format("订单ID: %d, 状态: %s, 总金额: %s",
                    orderId, statusDesc, order.getTotalAmount());

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 查询订单状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new OrderServiceException("查询订单状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean mockPaymentComplete(Long orderId) {
        try {
            log.info("🔄 模拟支付完成处理 - 订单ID: {}", orderId);

            // 模拟支付成功，直接调用支付成功处理逻辑
            BigDecimal mockPaymentAmount = orderService.getById(orderId).getPayAmount();
            Long mockPaymentId = System.currentTimeMillis(); // 模拟支付ID

            return handlePaymentSuccess(orderId, mockPaymentId, mockPaymentAmount);

        } catch (Exception e) {
            log.error("❌ 模拟支付完成失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentSuccess(Long orderId, Long paymentId, BigDecimal paymentAmount) {
        try {
            log.info("💰 处理支付成功事件 - 订单ID: {}, 支付ID: {}, 支付金额: {}",
                    orderId, paymentId, paymentAmount);

            // 1. 更新订单状态为已支付
            Order order = orderService.getById(orderId);
            if (order == null) {
                throw EntityNotFoundException.order(orderId);
            }

            if (order.getStatus() != 0) {
                throw new OrderStatusException(orderId, order.getStatus().toString(), "支付完成");
            }

            order.setStatus(1); // 设置为已支付状态
            boolean updated = orderService.updateById(order);
            if (!updated) {
                throw new OrderServiceException("更新订单支付状态失败");
            }

            // 2. 发送订单支付日志
            try {
                logCollectionProducer.sendOrderOperationLog(
                        orderId,
                        "ORDER_" + orderId,
                        order.getUserId(),
                        "PAY",
                        paymentAmount,
                        "SYSTEM"
                );
            } catch (Exception e) {
                log.warn("发送简单订单支付日志失败，订单ID：{}", orderId, e);
            }

            // 3. 发布订单完成事件（简化流程，支付成功即完成）
            publishOrderCompletedEvent(order, String.valueOf(paymentId));

            log.info("✅ 支付成功处理完成 - 订单ID: {}, 支付ID: {}", orderId, paymentId);
            return true;

        } catch (EntityNotFoundException | OrderStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 处理支付成功事件失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new OrderServiceException("处理支付成功事件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发布订单创建事件
     * 通知支付服务和库存服务处理订单
     */
    private void publishOrderCreatedEvent(Order order, OrderItem orderItem, String operator) {
        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");

            // 构建订单项信息
            OrderCreatedEvent.OrderItemInfo eventOrderItem = OrderCreatedEvent.OrderItemInfo.builder()
                    .productId(orderItem.getProductId())
                    .productName("商品_" + orderItem.getProductId()) // 简化商品名称
                    .unitPrice(orderItem.getPrice())
                    .quantity(orderItem.getQuantity())
                    .subtotal(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .build();

            // 构建订单创建事件
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNo("ORDER_" + order.getId()) // 简化订单号生成
                    .userId(order.getUserId())
                    .userName("User_" + order.getUserId()) // 简化用户名
                    .totalAmount(order.getTotalAmount())
                    .payAmount(order.getPayAmount())
                    .discountAmount(BigDecimal.ZERO) // 无优惠
                    .shippingFee(BigDecimal.ZERO) // 免运费
                    .paymentMethod(1) // 默认支付宝
                    .addressId(order.getAddressId())
                    .receiverName("收货人_" + order.getUserId()) // 简化收货人信息
                    .receiverPhone("138****8888")
                    .receiverAddress("默认收货地址")
                    .orderItems(Collections.singletonList(eventOrderItem))
                    .createTime(LocalDateTime.now())
                    .operator(operator)
                    .traceId(traceId)
                    .build();

            // 发布事件
            orderEventProducer.sendOrderCreatedEvent(event);

            log.info("📨 订单创建事件发布成功 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 发布订单创建事件失败 - 订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
            // 事件发布失败不应该影响订单创建的主流程
            // 可以考虑添加重试机制或补偿机制
        }
    }

    /**
     * 发布订单完成事件
     * 通知库存服务进行库存扣减
     */
    private void publishOrderCompletedEvent(Order order, String paymentId) {
        try {
            String traceId = UUID.randomUUID().toString().replace("-", "");

            // 构建订单完成事件
            OrderCompletedEvent event = OrderCompletedEvent.builder()
                    .orderId(order.getId())
                    .orderNo("ORDER_" + order.getId()) // 简化订单号生成
                    .userId(order.getUserId())
                    .userName("User_" + order.getUserId()) // 简化用户名
                    .totalAmount(order.getTotalAmount())
                    .payAmount(order.getPayAmount())
                    .orderStatus(1) // 已支付状态
                    .beforeStatus(0) // 之前是待支付状态
                    .afterStatus(1)  // 现在是已支付状态
                    .completedTime(LocalDateTime.now())
                    .operator("SYSTEM")
                    .traceId(traceId)
                    .build();

            // 发布事件
            orderEventProducer.sendOrderCompletedEvent(event);

            log.info("📨 订单完成事件发布成功 - 订单ID: {}, 追踪ID: {}", order.getId(), traceId);

        } catch (Exception e) {
            log.error("❌ 发布订单完成事件失败 - 订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
            // 事件发布失败不应该影响订单状态更新的主流程
        }
    }
}
