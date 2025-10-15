package com.cloud.order.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.order.enums.OrderRefundStatusEnum;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 订单消息消费者
 * 接收并处理订单相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderMessageProducer orderMessageProducer;

    /**
     * 消费支付成功事件
     * 当支付完成时，更新订单状态为已支付
     */
    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();

            log.info("📨 接收到支付成功事件: orderId={}, orderNo={}, paymentId={}, amount={}",
                    event.getOrderId(), event.getOrderNo(), event.getPaymentId(), event.getAmount());

            try {
                // 幂等性检查
                String eventId = event.getEventId();
                // TODO: 检查该事件是否已处理（可使用Redis存储已处理的eventId）

                // 更新订单状态为已支付
                boolean success = orderService.updateOrderStatusAfterPayment(
                        event.getOrderId(),
                        event.getPaymentId(),
                        event.getTransactionNo()
                );

                if (success) {
                    log.info("✅ 订单支付状态更新成功: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                } else {
                    log.error("❌ 订单支付状态更新失败: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    // TODO: 发送补偿消息或记录失败日志
                }

            } catch (Exception e) {
                log.error("❌ 处理支付成功事件失败: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                // TODO: 消息重试或死信队列处理
                throw new RuntimeException("处理支付成功事件失败", e);
            }
        };
    }

    /**
     * 消费库存冻结失败事件
     * 当库存冻结失败时，取消订单
     */
    @Bean
    public Consumer<Message<StockFreezeFailedEvent>> stockFreezeFailedConsumer() {
        return message -> {
            StockFreezeFailedEvent event = message.getPayload();

            log.warn("⚠️ 接收到库存冻结失败事件: orderId={}, orderNo={}, reason={}",
                    event.getOrderId(), event.getOrderNo(), event.getReason());

            try {
                // 取消订单
                boolean success = orderService.cancelOrderDueToStockFreezeFailed(
                        event.getOrderId(),
                        event.getReason()
                );

                if (success) {
                    log.info("✅ 订单已取消（库存冻结失败）: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                } else {
                    log.error("❌ 订单取消失败: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                }

            } catch (Exception e) {
                log.error("❌ 处理库存冻结失败事件异常: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                throw new RuntimeException("处理库存冻结失败事件异常", e);
            }
        };
    }

    /**
     * 消费退款完成事件
     * 当退款完成时，记录订单退款状态并恢复库存
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> refundCompletedConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");

            log.info("📨 接收到退款完成事件: orderId={}, orderNo={}, refundId={}, refundNo={}",
                    orderId, orderNo, refundId, refundNo);

            try {
                // 1. 查询订单商品列表
                LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderItem::getOrderId, orderId);
                List<OrderItem> orderItems = orderItemService.list(wrapper);

                if (orderItems == null || orderItems.isEmpty()) {
                    log.warn("⚠️ 订单没有商品明细，无需恢复库存: orderId={}", orderId);
                    return;
                }

                // 2. 构建商品数量映射
                Map<Long, Integer> productQuantityMap = new HashMap<>();
                for (OrderItem item : orderItems) {
                    productQuantityMap.put(item.getProductId(), item.getQuantity());
                }

                log.info("📦 准备恢复库存: orderId={}, 商品数量={}", orderId, orderItems.size());

                // 3. 发送库存恢复事件
                boolean sent = orderMessageProducer.sendStockRestoreEvent(
                        orderId,
                        orderNo,
                        refundId,
                        refundNo,
                        productQuantityMap
                );

                if (sent) {
                    log.info("✅ 库存恢复事件已发送: orderId={}, refundNo={}", orderId, refundNo);
                } else {
                    log.error("❌ 库存恢复事件发送失败: orderId={}, refundNo={}", orderId, refundNo);
                }

                // 4. 更新订单refund_status为"退款成功"
                Order order = orderService.getById(orderId);
                if (order != null) {
                    order.setRefundStatus(OrderRefundStatusEnum.REFUND_SUCCESS.getCode());
                    order.setUpdatedAt(LocalDateTime.now());
                    orderService.updateById(order);
                    log.info("✅ 订单退款状态已更新为退款成功: orderId={}", orderId);
                } else {
                    log.warn("⚠️ 订单不存在，无法更新退款状态: orderId={}", orderId);
                }

                log.info("✅ 退款完成事件处理成功: orderId={}, refundNo={}", orderId, refundNo);

            } catch (Exception e) {
                log.error("❌ 处理退款完成事件失败: orderId={}, refundNo={}",
                        orderId, refundNo, e);
                throw new RuntimeException("处理退款完成事件失败", e);
            }
        };
    }
}
