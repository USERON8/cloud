package com.cloud.merchant.service.impl;

import com.cloud.common.domain.MerchantChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商家日志消息服务类
 * 专门用于异步发送商家变更事件到RocketMQ，供日志服务消费并记录日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantLogMessageService {
    
    private final StreamBridge streamBridge;
    
    /**
     * 发送商家变更消息到RocketMQ
     * 
     * @param merchantId 商家ID
     * @param merchantName 商家名称
     * @param beforeStatus 变更前状态
     * @param afterStatus 变更后状态
     * @param changeType 变更类型 1-创建商家 2-更新商家 3-删除商家
     * @param operator 操作人
     */
    public void sendMerchantChangeMessage(Long merchantId, String merchantName, 
                                     Integer beforeStatus, Integer afterStatus,
                                     Integer changeType, String operator) {
        try {
            // 构建商家变更事件
            MerchantChangeEvent event = MerchantChangeEvent.builder()
                    .merchantId(merchantId)
                    .merchantName(merchantName)
                    .beforeStatus(beforeStatus)
                    .afterStatus(afterStatus)
                    .changeType(changeType)
                    .operator(operator)
                    .operateTime(LocalDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .build();
            
            // 通过StreamBridge发送消息到RocketMQ
            streamBridge.send("merchantLogProducer-out-0", event);
            log.info("发送商家变更消息成功，商家ID: {}, 变更类型: {}", merchantId, changeType);
        } catch (Exception e) {
            log.error("发送商家变更消息失败，商家ID: {}, 变更类型: {}", merchantId, changeType, e);
        }
    }
}