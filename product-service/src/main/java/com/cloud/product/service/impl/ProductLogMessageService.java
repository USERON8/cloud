package com.cloud.product.service.impl;

import com.cloud.common.domain.ProductChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品日志消息服务类
 * 专门用于异步发送商品变更事件到RocketMQ，供日志服务消费并记录日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLogMessageService {
    
    private final StreamBridge streamBridge;
    
    /**
     * 发送商品变更消息到RocketMQ
     * 
     * @param productId 商品ID
     * @param productName 商品名称
     * @param beforeCount 变更前数量
     * @param changeCount 变更数量
     * @param afterCount 变更后数量
     * @param changeType 变更类型 1-增加商品 2-更新商品 3-删除商品
     * @param operator 操作人
     */
    public void sendProductChangeMessage(Long productId, String productName, 
                                     Integer beforeCount, Integer changeCount, Integer afterCount,
                                     Integer changeType, String operator) {
        try {
            // 构建商品变更事件
            ProductChangeEvent event = ProductChangeEvent.builder()
                    .productId(productId)
                    .productName(productName)
                    .beforeCount(beforeCount)
                    .changeCount(changeCount)
                    .afterCount(afterCount)
                    .changeType(changeType)
                    .operator(operator)
                    .operateTime(LocalDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .build();
            
            // 通过StreamBridge发送消息到RocketMQ
            streamBridge.send("productLogProducer-out-0", event);
            log.info("发送商品变更消息成功，商品ID: {}, 变更类型: {}", productId, changeType);
        } catch (Exception e) {
            log.error("发送商品变更消息失败，商品ID: {}, 变更类型: {}", productId, changeType, e);
        }
    }
}