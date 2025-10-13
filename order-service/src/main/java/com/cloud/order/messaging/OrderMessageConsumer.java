package com.cloud.order.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

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
}
