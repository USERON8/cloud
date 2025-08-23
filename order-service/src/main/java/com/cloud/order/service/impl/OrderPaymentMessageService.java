package com.cloud.order.service.impl;

import com.cloud.common.domain.OrderChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentMessageService {

    private final StreamBridge streamBridge;


    public void sendOrderToPaymentMessage(Long orderId,
                                          Long userId,
                                          Integer beforeStatus,
                                          Integer afterStatus,
                                          Integer changeType,
                                          String operator) {
        try {
            // 构建订单变更事件
            OrderChangeEvent event = OrderChangeEvent.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .beforeStatus(beforeStatus)
                    .afterStatus(afterStatus)
                    .changeType(changeType)
                    .operator(operator)
                    .operateTime(LocalDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .build();

            // 通过StreamBridge发送消息到RocketMQ
            streamBridge.send("orderLogProducer-out-0", event);
            log.info("发送订单变更消息成功，订单ID: {}, 变更类型: {}", orderId, changeType);
        } catch (Exception e) {
            log.error("发送订单变更消息失败，订单ID: {}, 变更类型: {}", orderId, changeType, e);
        }
    }
}
