package com.cloud.payment.messaging;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 支付消息消费者
 * 接收并处理支付相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {

    private final PaymentService paymentService;
    private final PaymentMessageProducer paymentMessageProducer;

    /**
     * 消费订单创建事件
     * 创建支付并立即完成支付（简化逻辑）
     */
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();

            log.info("📨 接收到订单创建事件: orderId={}, orderNo={}, userId={}, totalAmount={}",
                    event.getOrderId(), event.getOrderNo(), event.getUserId(), event.getTotalAmount());

            try {
                // 幂等性检查
                String eventId = event.getEventId();
                // TODO: 检查该事件是否已处理（可使用Redis存储已处理的eventId）

                // 检查支付记录是否已存在
                if (paymentService.isPaymentRecordExists(event.getOrderId())) {
                    log.warn("⚠️ 订单支付记录已存在，跳过处理: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                // 第一步：创建支付
                log.info("💳 开始创建支付: orderId={}, orderNo={}, amount={}",
                        event.getOrderId(), event.getOrderNo(), event.getTotalAmount());

                PaymentDTO paymentDTO = new PaymentDTO();
                paymentDTO.setOrderId(event.getOrderId());
                paymentDTO.setOrderNo(event.getOrderNo());
                paymentDTO.setUserId(event.getUserId());
                paymentDTO.setAmount(event.getTotalAmount());
                paymentDTO.setPaymentMethod("ALIPAY"); // 默认使用支付宝
                paymentDTO.setStatus(0); // 待支付

                Long paymentId = paymentService.createPayment(paymentDTO);

                if (paymentId == null) {
                    log.error("❌ 创建支付失败: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    throw new RuntimeException("创建支付失败");
                }

                log.info("✅ 支付创建成功: paymentId={}, orderId={}, orderNo={}",
                        paymentId, event.getOrderId(), event.getOrderNo());

                // 第二步：立即完成支付（简化逻辑）
                log.info("💰 开始处理支付成功: paymentId={}, orderId={}, orderNo={}",
                        paymentId, event.getOrderId(), event.getOrderNo());

                Boolean success = paymentService.processPaymentSuccess(paymentId);

                if (!success) {
                    log.error("❌ 处理支付成功失败: paymentId={}, orderId={}, orderNo={}",
                            paymentId, event.getOrderId(), event.getOrderNo());
                    throw new RuntimeException("处理支付成功失败");
                }

                log.info("✅ 支付处理成功: paymentId={}, orderId={}, orderNo={}",
                        paymentId, event.getOrderId(), event.getOrderNo());

                // 第三步：发送支付成功事件
                log.info("📤 发送支付成功事件: paymentId={}, orderId={}, orderNo={}",
                        paymentId, event.getOrderId(), event.getOrderNo());

                String transactionNo = "TXN" + System.currentTimeMillis() + paymentId; // 生成流水号

                boolean sendResult = paymentMessageProducer.sendPaymentSuccessEvent(
                        paymentId,
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getUserId(),
                        event.getTotalAmount(),
                        "ALIPAY",
                        transactionNo
                );

                if (sendResult) {
                    log.info("🎉 订单支付流程完成: paymentId={}, orderId={}, orderNo={}, amount={}",
                            paymentId, event.getOrderId(), event.getOrderNo(), event.getTotalAmount());
                } else {
                    log.error("⚠️ 支付成功事件发送失败，但支付已完成: paymentId={}, orderId={}, orderNo={}",
                            paymentId, event.getOrderId(), event.getOrderNo());
                }

            } catch (Exception e) {
                log.error("❌ 处理订单创建事件失败: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                // 抛出异常触发消息重试
                throw new RuntimeException("处理订单创建事件失败", e);
            }
        };
    }
}
