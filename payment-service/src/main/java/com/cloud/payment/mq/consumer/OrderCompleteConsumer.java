package com.cloud.payment.mq.consumer;

import com.cloud.common.domain.event.OrderCompleteEvent;
import com.cloud.payment.exception.PaymentServiceException;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * 订单完成消息消费者
 * 监听订单服务发送的订单完成消息，用于完成支付记录
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompleteConsumer {

    private final PaymentService paymentService;

    /**
     * 订单完成消息消费者
     * 当订单服务完成订单时，会发送消息到ORDER_COMPLETE_TOPIC
     * 支付服务接收到消息后，需要完成相应的支付记录
     *
     * @return 消息消费者函数
     */
    @Bean
    @Transactional(rollbackFor = Exception.class)
    public Consumer<OrderCompleteEvent> orderCompleteMessageConsumer() {
        return event -> {
            try {
                log.info("接收到订单完成消息，订单ID: {}, 操作人: {}", event.getOrderId(), event.getOperator());

                // 完成支付记录
                boolean completed = paymentService.completePaymentForOrder(event);
                if (completed) {
                    log.info("支付记录完成成功，订单ID: {}, 操作人: {}", event.getOrderId(), event.getOperator());
                } else {
                    log.error("支付记录完成失败，订单ID: {}, 操作人: {}", event.getOrderId(), event.getOperator());
                    throw new PaymentServiceException("支付记录完成失败，订单ID: " + event.getOrderId());
                }

                log.info("订单完成消息处理完成，订单ID: {}", event.getOrderId());
            } catch (PaymentServiceException e) {
                log.error("处理订单完成消息时发生业务异常，订单ID: {}", event.getOrderId(), e);
                // 重新抛出异常以触发事务回滚
                throw e;
            } catch (Exception e) {
                log.error("处理订单完成消息时发生系统异常，订单ID: {}", event.getOrderId(), e);
                // 重新抛出异常以触发事务回滚
                throw e;
            }
        };
    }
}