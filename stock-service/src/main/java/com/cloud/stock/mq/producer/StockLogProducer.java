package com.cloud.stock.mq.producer;

import com.cloud.common.domain.StockChangeEvent;
import com.cloud.common.domain.StockTypeChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 库存日志消息服务类
 * 专门用于异步发送库存变更事件到RocketMQ，供日志服务消费并记录日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockLogProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送库存变更消息到RocketMQ
     *
     * @param productId   商品ID
     * @param productName 商品名称
     * @param beforeCount 变更前数量
     * @param changeCount 变更数量
     * @param afterCount  变更后数量
     * @param changeType  变更类型 1-增加库存 2-扣减库存 3-冻结库存 4-解冻库存
     * @param operator    操作人
     */
    public void sendStockCountChangeMessage(Long productId, String productName,
                                            Integer beforeCount, Integer changeCount, Integer afterCount,
                                            Integer changeType, String operator) {
        try {
            // 构建库存变更事件
            StockChangeEvent event = StockChangeEvent.builder()
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
            streamBridge.send("stockLogProducer-out-0", event);
            log.info("发送库存变更消息成功，商品ID: {}, 变更类型: {}", productId, changeType);
        } catch (Exception e) {
            log.error("发送库存变更消息失败，商品ID: {}, 变更类型: {}", productId, changeType, e);
        }
    }

    /**
     * 发送库存类型变更消息到RocketMQ
     *
     * @param stockId     库存ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param changeType  变更类型 1-创建库存类型 2-更新库存类型 3-删除库存类型
     * @param operator    操作人
     */
    public void sendStockTypeChangeMessage(Long stockId,
                                           Long productId,
                                           String productName,
                                           Integer changeType,
                                           String operator) {
        try {
            // 构建库存类型变更事件
            StockTypeChangeEvent event = StockTypeChangeEvent.builder()
                    .stockId(stockId)
                    .productId(productId)
                    .productName(productName)
                    .changeType(changeType)
                    .operator(operator)
                    .operateTime(LocalDateTime.now())
                    .traceId(UUID.randomUUID().toString())
                    .build();

            // 通过StreamBridge发送消息到RocketMQ
            streamBridge.send("stockTypeChangeProducer-out-0", event);
            log.info("发送库存类型变更消息成功，库存ID: {}, 商品ID: {}, 变更类型: {}", stockId, productId, changeType);
        } catch (Exception e) {
            log.error("发送库存类型变更消息失败，库存ID: {}, 商品ID: {}, 变更类型: {}", stockId, productId, changeType, e);
        }
    }
}