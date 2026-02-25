package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;








@Slf4j
@Component
public class ContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                
                runnable.run();

            } catch (Exception e) {
                
                String traceId = MDC.get("traceId");
                String userId = MDC.get("userId");
                log.error("- traceId: {}, userId: {}, error: {}",
                        traceId, userId, e.getMessage(), e);
                throw e;
            } finally {
                
                MDC.clear();
            }
        };
    }
}
