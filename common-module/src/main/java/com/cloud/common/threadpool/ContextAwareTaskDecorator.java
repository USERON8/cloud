package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 上下文感知任务装饰器
 * 用于在线程池异步执行任务时传递MDC上下文信息
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class ContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 获取当前线程的MDC上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 将MDC上下文设置到异步线程中
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                // 执行原始任务
                runnable.run();

            } catch (Exception e) {
                // 记录异步任务执行异常，包含MDC上下文信息
                String traceId = MDC.get("traceId");
                String userId = MDC.get("userId");
                log.error("异步任务执行异常 - traceId: {}, userId: {}, error: {}",
                    traceId, userId, e.getMessage(), e);
                throw e;
            } finally {
                // 清理MDC上下文，避免内存泄漏
                MDC.clear();
            }
        };
    }
}