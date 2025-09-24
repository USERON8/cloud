package com.cloud.product.messaging.producer;

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

/**
 * 商品服务日志收集生产者
 * 负责发送商品变更日志到日志服务
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class ProductLogProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送商品变更日志
     *
     * @param productId   商品ID
     * @param productName 商品名称
     * @param operation   操作类型
     * @param beforeData  变更前数据
     * @param afterData   变更后数据
     * @param operator    操作人
     */
    public void sendProductChangeLog(Long productId, String productName, String operation,
                                     String beforeData, String afterData, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("product-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("PRODUCT_MANAGEMENT")
                .operation(operation)
                .description("商品" + operation)
                .businessId(productId.toString())
                .businessType("PRODUCT")
                .beforeData(beforeData)
                .afterData(afterData)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator + ", 商品名称: " + productName)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.PRODUCT_LOG);
    }

    /**
     * 发送商品创建日志
     *
     * @param productId   商品ID
     * @param productName 商品名称
     * @param shopId      店铺ID
     * @param operator    操作人
     */
    public void sendProductCreateLog(Long productId, String productName, Long shopId, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("product-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("PRODUCT_CREATE")
                .operation("CREATE")
                .description("商品创建")
                .businessId(productId.toString())
                .businessType("PRODUCT")
                .content(String.format("创建商品: ID=%d, 名称=%s, 店铺ID=%d", productId, productName, shopId))
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.PRODUCT_LOG);
    }

    /**
     * 发送商品状态变更日志
     *
     * @param productId    商品ID
     * @param productName  商品名称
     * @param beforeStatus 变更前状态
     * @param afterStatus  变更后状态
     * @param operator     操作人
     */
    public void sendProductStatusLog(Long productId, String productName, Integer beforeStatus,
                                     Integer afterStatus, String operator) {
        String statusDesc = getStatusDescription(beforeStatus) + " → " + getStatusDescription(afterStatus);

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("product-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("PRODUCT_STATUS")
                .operation("STATUS_CHANGE")
                .description("商品状态变更")
                .businessId(productId.toString())
                .businessType("PRODUCT")
                .content(String.format("商品状态变更: %s(%s)", productName, statusDesc))
                .beforeData(String.format("{\"status\":%d}", beforeStatus))
                .afterData(String.format("{\"status\":%d}", afterStatus))
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.PRODUCT_LOG);
    }

    /**
     * 发送商品异常日志
     *
     * @param productId        商品ID
     * @param operation        操作类型
     * @param exceptionMessage 异常信息
     * @param exceptionStack   异常堆栈
     */
    public void sendProductErrorLog(Long productId, String operation, String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("product-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("PRODUCT_SERVICE")
                .operation(operation)
                .description("商品服务异常")
                .businessId(productId != null ? productId.toString() : null)
                .businessType("PRODUCT")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendLogEvent(event, MessageTopicConstants.LogTags.PRODUCT_LOG);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "PRODUCT_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "product-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息
            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 商品日志发送成功 - 操作: {}, 商品ID: {}, 追踪ID: {}",
                        event.getOperation(), event.getBusinessId(), traceId);
            } else {
                log.error("❌ 商品日志发送失败 - 操作: {}, 商品ID: {}, 追踪ID: {}",
                        event.getOperation(), event.getBusinessId(), traceId);
                throw new MessageSendException("商品日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送商品日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            // 日志发送失败不应影响主业务，这里只记录错误
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "下架";
            case 1 -> "上架";
            case 2 -> "缺货";
            default -> "未知(" + status + ")";
        };
    }

    /**
     * 生成日志ID
     */
    private String generateLogId() {
        return com.cloud.common.utils.StringUtils.generateLogId("PRODUCT");
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }
}
