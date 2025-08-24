package com.cloud.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 统一日志工具类
 * 提供结构化的日志记录方法
 */
public class LogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    /**
     * 添加跟踪ID到MDC
     */
    public static void addTraceId() {
        MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 从MDC中移除跟踪ID
     */
    public static void removeTraceId() {
        MDC.remove("traceId");
    }

    /**
     * 记录业务操作日志
     * @param operation 操作类型
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @param message 日志消息
     */
    public static void logOperation(String operation, String userId, String resourceId, String message) {
        logger.info("Operation: {}, UserId: {}, ResourceId: {}, Message: {}", operation, userId, resourceId, message);
    }

    /**
     * 记录错误日志
     * @param operation 操作类型
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @param errorMessage 错误消息
     * @param throwable 异常对象
     */
    public static void logError(String operation, String userId, String resourceId, String errorMessage, Throwable throwable) {
        logger.error("Operation: {}, UserId: {}, ResourceId: {}, ErrorMessage: {}", 
                     operation, userId, resourceId, errorMessage, throwable);
    }

    /**
     * 记录调试日志
     * @param message 调试消息
     */
    public static void logDebug(String message) {
        logger.debug(message);
    }

    /**
     * 记录信息日志
     * @param message 信息消息
     */
    public static void logInfo(String message) {
        logger.info(message);
    }

    /**
     * 记录警告日志
     * @param message 警告消息
     */
    public static void logWarn(String message) {
        logger.warn(message);
    }
}