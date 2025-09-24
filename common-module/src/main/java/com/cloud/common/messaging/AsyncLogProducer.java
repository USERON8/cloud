package com.cloud.common.messaging;

import com.cloud.common.domain.event.LogCollectionEvent;
import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 异步日志生产者
 * 专门用于异步发送日志消息，不阻塞主业务流程
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncLogProducer {

    private static final String LOG_BINDING_NAME = "logProducer-out-0";
    private final AsyncMessageProducer asyncMessageProducer;

    /**
     * 异步发送业务操作日志
     *
     * @param serviceName  服务名称
     * @param module       模块名称
     * @param operation    操作类型
     * @param description  操作描述
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param beforeData   变更前数据
     * @param afterData    变更后数据
     * @param operator     操作人
     * @param remark       备注
     */
    public void sendBusinessLogAsync(String serviceName, String module, String operation,
                                     String description, String businessId, String businessType,
                                     String beforeData, String afterData, String operator, String remark) {

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(StringUtils.generateLogId(serviceName.toUpperCase()))
                .serviceName(serviceName)
                .logLevel("INFO")
                .logType("BUSINESS")
                .module(module)
                .operation(operation)
                .description(description)
                .businessId(businessId)
                .businessType(businessType)
                .beforeData(beforeData)
                .afterData(afterData)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(StringUtils.generateTraceId())
                .remark(remark != null ? remark : ("操作人: " + operator))
                .build();

        // 异步发送，不阻塞主业务
        asyncMessageProducer.sendAsyncSilent(
                LOG_BINDING_NAME,
                event,
                "BUSINESS_LOG",
                "LOG_" + event.getLogId(),
                "LOG_COLLECTION"
        );
    }

    /**
     * 异步发送用户操作日志
     */
    public void sendUserOperationLogAsync(String serviceName, String operation, Long userId,
                                          String userName, String userType, String beforeData,
                                          String afterData, String operator) {

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(StringUtils.generateLogId("USER"))
                .serviceName(serviceName)
                .logLevel("INFO")
                .logType("USER_OPERATION")
                .module("USER_MANAGEMENT")
                .operation(operation)
                .description("用户" + operation + "操作")
                .userId(userId)
                .userName(userName)
                .userType(userType)
                .businessId(userId != null ? userId.toString() : null)
                .businessType("USER")
                .beforeData(beforeData)
                .afterData(afterData)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(StringUtils.generateTraceId())
                .remark("操作人: " + operator)
                .build();

        asyncMessageProducer.sendAsyncSilent(
                LOG_BINDING_NAME,
                event,
                "USER_LOG",
                "USER_LOG_" + event.getLogId(),
                "LOG_COLLECTION"
        );
    }

    /**
     * 异步发送系统操作日志
     */
    public void sendSystemLogAsync(String serviceName, String module, String operation,
                                   String description, String result, String errorMessage) {

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(StringUtils.generateLogId("SYSTEM"))
                .serviceName(serviceName)
                .logLevel("ERROR".equals(result) ? "ERROR" : "INFO")
                .logType("SYSTEM")
                .module(module)
                .operation(operation)
                .description(description)
                .result(result)
                .exceptionMessage(errorMessage)
                .createTime(LocalDateTime.now())
                .traceId(StringUtils.generateTraceId())
                .build();

        asyncMessageProducer.sendAsyncSilent(
                LOG_BINDING_NAME,
                event,
                "SYSTEM_LOG",
                "SYSTEM_LOG_" + event.getLogId(),
                "LOG_COLLECTION"
        );
    }

    /**
     * 异步发送性能监控日志
     */
    public void sendPerformanceLogAsync(String serviceName, String operation, Long responseTime,
                                        String requestPath, String method, Integer resultCount) {

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(StringUtils.generateLogId("PERF"))
                .serviceName(serviceName)
                .logLevel("INFO")
                .logType("PERFORMANCE")
                .module("PERFORMANCE_MONITOR")
                .operation(operation)
                .description("性能监控")
                .responseTime(responseTime)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(StringUtils.generateTraceId())
                .remark(String.format("请求路径: %s, 方法: %s, 结果数量: %d",
                        requestPath, method, resultCount != null ? resultCount : 0))
                .build();

        asyncMessageProducer.sendAsyncSilent(
                LOG_BINDING_NAME,
                event,
                "PERFORMANCE_LOG",
                "PERF_LOG_" + event.getLogId(),
                "LOG_COLLECTION"
        );
    }

    /**
     * 异步发送安全审计日志
     */
    public void sendSecurityLogAsync(String serviceName, String operation, Long userId,
                                     String userName, String ipAddress, String userAgent,
                                     String result, String remark) {

        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(StringUtils.generateLogId("SECURITY"))
                .serviceName(serviceName)
                .logLevel("FAILED".equals(result) ? "WARN" : "INFO")
                .logType("SECURITY")
                .module("SECURITY_AUDIT")
                .operation(operation)
                .description("安全审计")
                .userId(userId)
                .userName(userName)
                .result(result)
                .createTime(LocalDateTime.now())
                .traceId(StringUtils.generateTraceId())
                .remark(String.format("IP: %s, UserAgent: %s, %s",
                        ipAddress, userAgent, remark != null ? remark : ""))
                .build();

        // 安全日志使用带重试的异步发送
        asyncMessageProducer.sendAsyncWithRetry(
                LOG_BINDING_NAME,
                event,
                "SECURITY_LOG",
                "SEC_LOG_" + event.getLogId(),
                "LOG_COLLECTION",
                2 // 最多重试2次
        );
    }

    /**
     * 异步批量发送日志
     */
    public CompletableFuture<Integer> sendBatchLogsAsync(java.util.List<LogCollectionEvent> events) {
        return asyncMessageProducer.sendBatchAsync(
                LOG_BINDING_NAME,
                events,
                "BATCH_LOG",
                "BATCH_LOG_COLLECTION"
        );
    }
}
