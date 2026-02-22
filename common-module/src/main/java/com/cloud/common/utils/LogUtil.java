package com.cloud.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;









public class LogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    






    public static void addTraceId() {
        MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
    }

    






    public static void removeTraceId() {
        MDC.remove("traceId");
    }

    







    public static void logOperation(String operation, String userId, String resourceId, String message) {
        logger.info("Operation: {}, UserId: {}, ResourceId: {}, Message: {}", operation, userId, resourceId, message);
    }

    








    public static void logError(String operation, String userId, String resourceId, String errorMessage, Throwable throwable) {
        logger.error("Operation: {}, UserId: {}, ResourceId: {}, ErrorMessage: {}",
                operation, userId, resourceId, errorMessage, throwable);
    }

    




    public static void logDebug(String message) {
        logger.debug(message);
    }

    




    public static void logInfo(String message) {
        logger.info(message);
    }

    




    public static void logWarn(String message) {
        logger.warn(message);
    }
}
