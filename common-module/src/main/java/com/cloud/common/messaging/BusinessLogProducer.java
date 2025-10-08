package com.cloud.common.messaging;

import com.cloud.common.domain.event.LogCollectionEvent;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统一业务日志生产者
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BusinessLogProducer {

    private static final String LOG_BINDING_NAME = "logProducer-out-0";
    private final AsyncMessageProducer asyncMessageProducer;

    /**
     * 发送用户操作日志
     *
     * @param userId    用户ID
     * @param operation 操作类型
     * @param details   操作详情
     */
    public void sendUserOperationLog(Long userId, String operation, String details) {
        try {
            log.info("用户操作日志 - userId: {}, operation: {}, details: {}", userId, operation, details);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("USER"))
                    .serviceName("common-service")
                    .logLevel("INFO")
                    .logType("USER_OPERATION")
                    .module("USER_MANAGEMENT")
                    .operation(operation)
                    .description("用户" + operation + "操作")
                    .userId(userId)
                    .userName("User_" + userId)
                    .businessId(userId != null ? userId.toString() : null)
                    .businessType("USER")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(StringUtils.generateTraceId())
                    .remark(details)
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "USER_OPERATION_LOG",
                    "USER_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送用户操作日志失败 - userId: {}, operation: {}, 错误: {}", userId, operation, e.getMessage());
        }
    }

    /**
     * 发送系统日志
     *
     * @param level   日志级别
     * @param message 日志消息
     */
    public void sendSystemLog(String level, String message) {
        try {
            log.info("系统日志 - level: {}, message: {}", level, message);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("SYSTEM"))
                    .serviceName("common-service")
                    .logLevel(level != null ? level.toUpperCase() : "INFO")
                    .logType("SYSTEM")
                    .module("SYSTEM_MONITOR")
                    .operation("SYSTEM_LOG")
                    .description("系统日志记录")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(StringUtils.generateTraceId())
                    .content(message)
                    .remark("系统自动记录")
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "SYSTEM_LOG",
                    "SYSTEM_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送系统日志失败 - level: {}, message: {}, 错误: {}", level, message, e.getMessage());
        }
    }

    /**
     * 发送订单完成日志
     */
    public void sendOrderCompletedLog(String traceId, Long orderId, String orderNo,
                                      Long userId, String userName, java.math.BigDecimal totalAmount,
                                      java.math.BigDecimal payAmount, Integer orderStatus, Long shopId,
                                      String completedTime, String operator, String operatorId) {
        try {
            log.info("订单完成日志 - orderId: {}, orderNo: {}, userId: {}, amount: {}",
                    orderId, orderNo, userId, payAmount);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("ORDER"))
                    .serviceName("order-service")
                    .logLevel("INFO")
                    .logType("BUSINESS")
                    .module("ORDER_MANAGEMENT")
                    .operation("ORDER_COMPLETED")
                    .description("订单完成")
                    .userId(userId)
                    .userName(userName)
                    .businessId(orderId != null ? orderId.toString() : null)
                    .businessType("ORDER")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(traceId != null ? traceId : StringUtils.generateTraceId())
                    .beforeData(String.format("{\"orderStatus\":%d}", orderStatus - 1))
                    .afterData(String.format("{\"orderStatus\":%d,\"completedTime\":\"%s\"}", orderStatus, completedTime))
                    .remark(String.format("订单号: %s, 总金额: %s, 实付金额: %s, 店铺ID: %s, 操作人: %s",
                            orderNo, totalAmount, payAmount, shopId, operator))
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "ORDER_COMPLETED_LOG",
                    "ORDER_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送订单完成日志失败 - orderId: {}, 错误: {}", orderId, e.getMessage());
        }
    }

    /**
     * 发送订单退款日志
     */
    public void sendOrderRefundLog(String traceId, Long orderId, String orderNo,
                                   Long userId, String userName, java.math.BigDecimal refundAmount,
                                   String refundReason, String refundTime, Long shopId,
                                   String operator, String operatorId) {
        try {
            log.info("订单退款日志 - orderId: {}, orderNo: {}, userId: {}, refundAmount: {}",
                    orderId, orderNo, userId, refundAmount);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("ORDER"))
                    .serviceName("order-service")
                    .logLevel("INFO")
                    .logType("BUSINESS")
                    .module("ORDER_MANAGEMENT")
                    .operation("ORDER_REFUND")
                    .description("订单退款")
                    .userId(userId)
                    .userName(userName)
                    .businessId(orderId != null ? orderId.toString() : null)
                    .businessType("ORDER")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(traceId != null ? traceId : StringUtils.generateTraceId())
                    .beforeData(String.format("{\"orderStatus\":\"PAID\"}"))
                    .afterData(String.format("{\"orderStatus\":\"REFUNDED\",\"refundTime\":\"%s\",\"refundAmount\":%s}",
                            refundTime, refundAmount))
                    .remark(String.format("订单号: %s, 退款金额: %s, 退款原因: %s, 店铺ID: %s, 操作人: %s",
                            orderNo, refundAmount, refundReason, shopId, operator))
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "ORDER_REFUND_LOG",
                    "ORDER_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送订单退款日志失败 - orderId: {}, 错误: {}", orderId, e.getMessage());
        }
    }

    /**
     * 发送支付成功日志
     */
    public void sendPaymentSuccessLog(String serviceName, Long paymentId, Long orderId, String orderNo,
                                      Long userId, String userName, java.math.BigDecimal amount,
                                      String paymentMethod, String thirdPartyTradeNo, String operator) {
        try {
            log.info("支付成功日志 - paymentId: {}, orderId: {}, orderNo: {}, userId: {}, amount: {}, method: {}",
                    paymentId, orderId, orderNo, userId, amount, paymentMethod);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("PAYMENT"))
                    .serviceName(serviceName != null ? serviceName : "payment-service")
                    .logLevel("INFO")
                    .logType("BUSINESS")
                    .module("PAYMENT_MANAGEMENT")
                    .operation("PAYMENT_SUCCESS")
                    .description("支付成功")
                    .userId(userId)
                    .userName(userName)
                    .businessId(paymentId != null ? paymentId.toString() : null)
                    .businessType("PAYMENT")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(StringUtils.generateTraceId())
                    .beforeData(String.format("{\"paymentStatus\":\"PENDING\"}"))
                    .afterData(String.format("{\"paymentStatus\":\"SUCCESS\",\"amount\":%s,\"paymentMethod\":\"%s\"}",
                            amount, paymentMethod))
                    .remark(String.format("订单号: %s, 支付金额: %s, 支付方式: %s, 第三方交易号: %s, 操作人: %s",
                            orderNo, amount, paymentMethod, thirdPartyTradeNo, operator))
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "PAYMENT_SUCCESS_LOG",
                    "PAYMENT_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送支付成功日志失败 - paymentId: {}, 错误: {}", paymentId, e.getMessage());
        }
    }

    /**
     * 发送支付退款日志
     */
    public void sendPaymentRefundLog(String serviceName, Long paymentId, Long refundId, Long orderId,
                                     String orderNo, Long userId, String userName, java.math.BigDecimal refundAmount,
                                     String refundReason, String refundType, String paymentMethod, String operator) {
        try {
            log.info("支付退款日志 - paymentId: {}, refundId: {}, orderId: {}, orderNo: {}, userId: {}, refundAmount: {}",
                    paymentId, refundId, orderId, orderNo, userId, refundAmount);

            LogCollectionEvent event = LogCollectionEvent.builder()
                    .logId(StringUtils.generateLogId("PAYMENT"))
                    .serviceName(serviceName != null ? serviceName : "payment-service")
                    .logLevel("INFO")
                    .logType("BUSINESS")
                    .module("PAYMENT_MANAGEMENT")
                    .operation("PAYMENT_REFUND")
                    .description("支付退款")
                    .userId(userId)
                    .userName(userName)
                    .businessId(paymentId != null ? paymentId.toString() : null)
                    .businessType("PAYMENT")
                    .result("SUCCESS")
                    .createTime(LocalDateTime.now())
                    .traceId(StringUtils.generateTraceId())
                    .beforeData(String.format("{\"paymentStatus\":\"SUCCESS\"}"))
                    .afterData(String.format("{\"paymentStatus\":\"REFUNDED\",\"refundAmount\":%s,\"refundType\":\"%s\"}",
                            refundAmount, refundType))
                    .remark(String.format("订单号: %s, 退款金额: %s, 退款原因: %s, 退款类型: %s, 支付方式: %s, 操作人: %s",
                            orderNo, refundAmount, refundReason, refundType, paymentMethod, operator))
                    .build();

            // 异步发送，不阻塞主业务
            asyncMessageProducer.sendAsyncSilent(
                    LOG_BINDING_NAME,
                    event,
                    "PAYMENT_REFUND_LOG",
                    "PAYMENT_LOG_" + event.getLogId(),
                    "LOG_COLLECTION"
            );

        } catch (Exception e) {
            log.warn("发送支付退款日志失败 - paymentId: {}, refundId: {}, 错误: {}", paymentId, refundId, e.getMessage());
        }
    }

}
