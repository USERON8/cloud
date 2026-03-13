package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;



@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener(txProducerGroup = PaymentSuccessTxProducer.TX_GROUP)
public class PaymentSuccessTransactionListener implements RocketMQLocalTransactionListener {

    private final PaymentOrderMapper paymentOrderMapper;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        PaymentSuccessEvent event = resolveEvent(msg, arg);
        return resolveState(event);
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        PaymentSuccessEvent event = resolveEvent(msg, null);
        return resolveState(event);
    }

    private RocketMQLocalTransactionState resolveState(PaymentSuccessEvent event) {
        if (event == null || event.getPaymentId() == null) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        PaymentOrderEntity order = paymentOrderMapper.selectById(event.getPaymentId());
        if (order == null) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        if ("PAID".equals(order.getStatus())) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        if ("FAILED".equals(order.getStatus())) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        return RocketMQLocalTransactionState.UNKNOWN;
    }

    private PaymentSuccessEvent resolveEvent(Message msg, Object arg) {
        if (arg instanceof PaymentSuccessEvent payload) {
            return payload;
        }
        if (msg != null && msg.getPayload() instanceof PaymentSuccessEvent payload) {
            return payload;
        }
        return null;
    }
}
