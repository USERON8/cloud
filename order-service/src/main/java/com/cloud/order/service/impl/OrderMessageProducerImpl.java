package com.cloud.order.service.impl;

import com.cloud.common.domain.OrderChangeEvent;
import com.cloud.order.service.OrderMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单日志消息服务类
 * 专门用于异步发送订单变更事件到RocketMQ，供日志服务消费并记录日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMessageProducerImpl implements OrderMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送订单变更消息到RocketMQ
     *
     * @param orderId      订单ID
     * @param userId       用户ID
     * @param beforeStatus 变更前状态
     * @param afterStatus  变更后状态
     * @param changeType   变更类型 1-创建订单 2-更新订单 3-删除订单
     * @param operator     操作人
     */
    public void sendOrderChangeMessage(Long orderId, Long userId,
                                       Integer beforeStatus, Integer afterStatus,
                                       Integer changeType, String operator) {
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

    @Override
    public MessageChannel orderLogOutput() {
        return null;
    }

    @Override
    public MessageChannel paymentCreateOutput() {
        return null;
    }

    @Override
    public MessageChannel stockFreezeOutput() {
        return null;
    }
}