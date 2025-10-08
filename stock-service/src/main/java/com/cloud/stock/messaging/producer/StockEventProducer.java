package com.cloud.stock.messaging.producer;

import com.cloud.common.domain.event.stock.StockChangeEvent;
import com.cloud.common.exception.MessageSendException;
import com.cloud.common.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存事件生产者
 * 负责发送库存变更事件到RocketMQ
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockEventProducer {

    private static final String STOCK_BINDING_NAME = "stock-producer-out-0";
    private final StreamBridge streamBridge;

    /**
     * 生产入库事件
     */
    public void produceStockIn(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_IN", "stock-in");
    }

    /**
     * 生产出库事件
     */
    public void produceStockOut(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_OUT", "stock-out");
    }

    /**
     * 生产库存预扣事件
     */
    public void produceStockReserved(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_RESERVED", "stock-reserved");
    }

    /**
     * 生产释放预扣事件
     */
    public void produceStockReleased(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_RELEASED", "stock-released");
    }

    /**
     * 生产库存锁定事件
     */
    public void produceStockLocked(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_LOCKED", "stock-locked");
    }

    /**
     * 生产库存解锁事件
     */
    public void produceStockUnlocked(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_UNLOCKED", "stock-unlocked");
    }

    /**
     * 生产盘点调整事件
     */
    public void produceStockAdjusted(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_ADJUSTED", "stock-adjusted");
    }

    /**
     * 生产库存调拨事件
     */
    public void produceStockTransferred(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_TRANSFERRED", "stock-transferred");
    }

    /**
     * 生产库存冻结事件
     */
    public void produceStockFrozen(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_FROZEN", "stock-frozen");
    }

    /**
     * 生产库存解冻事件
     */
    public void produceStockUnfrozen(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_UNFROZEN", "stock-unfrozen");
    }

    /**
     * 生产库存报废事件
     */
    public void produceStockScraped(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_SCRAPED", "stock-scraped");
    }

    /**
     * 生产库存警告事件
     */
    public void produceStockWarning(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_WARNING", "stock-warning");
    }

    /**
     * 生产库存同步事件
     */
    public void produceStockSync(StockChangeEvent event) {
        sendStockEvent(event, "STOCK_SYNC", "stock-sync");
    }

    /**
     * 统一发送库存事件的内部方法
     * 按照官方示例标准实现，使用GenericMessage和MessageConst
     */
    private void sendStockEvent(StockChangeEvent event, String changeType, String tag) {
        try {
            // 按照官方示例构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "STOCK_" + event.getStockId());
            headers.put("eventType", changeType);
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息（官方标准方式）
            Message<StockChangeEvent> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("stock-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(STOCK_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 库存事件生产成功 - 事件类型: {}, 库存ID: {}, 商品ID: {}, 商品名称: {}, 操作人: {}, Tag: {}, TraceId: {}",
                        changeType, event.getStockId(), event.getProductId(), event.getProductName(),
                        event.getOperator(), tag, traceId);
            } else {
                log.error("❌ 库存事件生产失败 - 事件类型: {}, 库存ID: {}, TraceId: {}",
                        changeType, event.getStockId(), traceId);
                throw new MessageSendException("库存事件生产失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存事件时发生异常 - 事件类型: {}, 库存ID: {}, 错误: {}",
                    changeType, event.getStockId(), e.getMessage(), e);
            throw new MessageSendException("发送库存事件异常", e);
        }
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
