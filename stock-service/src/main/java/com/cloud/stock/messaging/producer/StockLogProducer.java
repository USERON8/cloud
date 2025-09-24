package com.cloud.stock.messaging.producer;

import com.cloud.common.constant.MessageTopicConstants;
import com.cloud.common.domain.event.LogCollectionEvent;
import com.cloud.common.exception.MessageSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 库存服务日志收集生产者
 * 负责发送库存变更日志到日志服务
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class StockLogProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送库存变更日志
     *
     * @param productId  商品ID
     * @param operation  操作类型
     * @param beforeData 变更前数据
     * @param afterData  变更后数据
     * @param operator   操作人
     */
    public void sendStockChangeLog(Long productId, String operation, String beforeData, String afterData, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("STOCK_MANAGEMENT")
                .operation(operation)
                .description("库存" + operation)
                .businessId(productId.toString())
                .businessType("STOCK")
                .beforeData(beforeData)
                .afterData(afterData)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 发送库存冻结日志
     *
     * @param productId 商品ID
     * @param quantity  冻结数量
     * @param orderNo   订单号
     * @param operator  操作人
     */
    public void sendStockFreezeLog(Long productId, Integer quantity, String orderNo, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("STOCK_FREEZE")
                .operation("FREEZE")
                .description("库存冻结")
                .businessId(productId.toString())
                .businessType("STOCK")
                .content(String.format("冻结商品ID:%d，数量:%d，订单号:%s", productId, quantity, orderNo))
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 发送库存扣减日志
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @param orderNo   订单号
     * @param operator  操作人
     */
    public void sendStockDeductLog(Long productId, Integer quantity, String orderNo, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("STOCK_DEDUCT")
                .operation("DEDUCT")
                .description("库存扣减")
                .businessId(productId.toString())
                .businessType("STOCK")
                .content(String.format("扣减商品ID:%d，数量:%d，订单号:%s", productId, quantity, orderNo))
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 发送库存解冻日志
     *
     * @param productId 商品ID
     * @param quantity  解冻数量
     * @param orderNo   订单号
     * @param operator  操作人
     */
    public void sendStockUnfreezeLog(Long productId, Integer quantity, String orderNo, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("STOCK_UNFREEZE")
                .operation("UNFREEZE")
                .description("库存解冻")
                .businessId(productId.toString())
                .businessType("STOCK")
                .content(String.format("解冻商品ID:%d，数量:%d，订单号:%s", productId, quantity, orderNo))
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 发送库存异常日志
     *
     * @param productId        商品ID
     * @param operation        操作类型
     * @param exceptionMessage 异常信息
     * @param exceptionStack   异常堆栈
     */
    public void sendStockErrorLog(Long productId, String operation, String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("STOCK_SERVICE")
                .operation(operation)
                .description("库存服务异常")
                .businessId(productId != null ? productId.toString() : null)
                .businessType("STOCK")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "STOCK_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "stock-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息
            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 库存日志发送成功 - 操作: {}, 商品ID: {}, 追踪ID: {}",
                        event.getOperation(), event.getBusinessId(), traceId);
            } else {
                log.error("❌ 库存日志发送失败 - 操作: {}, 商品ID: {}, 追踪ID: {}",
                        event.getOperation(), event.getBusinessId(), traceId);
                throw new MessageSendException("库存日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            // 日志发送失败不应影响主业务，这里只记录错误
        }
    }

    /**
     * 生成日志ID
     */
    private String generateLogId() {
        return "STOCK_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
