package com.cloud.payment.messaging;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundProcessConsumer {

    private static final String NS_REFUND_PROCESS = "payment:refund:process";

    private final MessageIdempotencyService messageIdempotencyService;
    private final PaymentOrderService paymentOrderService;
    private final TradeMetrics tradeMetrics;

    @Bean
    public Consumer<Message<PaymentRefundCommandDTO>> refundProcessConsumer() {
        return message -> {
            PaymentRefundCommandDTO command = message.getPayload();
            String eventId = resolveEventId(command);
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_PROCESS, eventId)) {
                log.warn("Duplicate refund process event, skip: eventId={}", eventId);
                return;
            }

            try {
                if (command == null) {
                    tradeMetrics.incrementMessageConsume("refund_process", "failed");
                    messageIdempotencyService.markSuccess(NS_REFUND_PROCESS, eventId);
                    return;
                }
                paymentOrderService.createRefund(command);
                tradeMetrics.incrementMessageConsume("refund_process", "success");
                messageIdempotencyService.markSuccess(NS_REFUND_PROCESS, eventId);
            } catch (Exception ex) {
                tradeMetrics.incrementMessageConsume("refund_process", "retry");
                log.error("Handle refund process failed: eventId={}, refundNo={}", eventId,
                        command == null ? null : command.getRefundNo(), ex);
                throw new RuntimeException("Handle refund process failed", ex);
            }
        };
    }

    private String resolveEventId(PaymentRefundCommandDTO command) {
        if (command == null) {
            return "REFUND_PROCESS:" + System.currentTimeMillis();
        }
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
            return command.getIdempotencyKey();
        }
        if (command.getRefundNo() != null && !command.getRefundNo().isBlank()) {
            return "REFUND_PROCESS:" + command.getRefundNo();
        }
        return "REFUND_PROCESS:" + System.currentTimeMillis();
    }
}
