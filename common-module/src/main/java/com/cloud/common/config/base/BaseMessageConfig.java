package com.cloud.common.config.base;

import com.cloud.common.config.properties.MessageProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.Map;








@Slf4j
@Configuration
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public abstract class BaseMessageConfig {

    @Resource
    protected StreamBridge streamBridge;

    @Autowired(required = false)
    protected MessageProperties messageProperties;

    public BaseMessageConfig() {
        
    }

    




    protected abstract String getServiceName();

    







    protected Map<String, Object> createMessageHeaders(String tag, String key, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("eventType", eventType);

        
        if (messageProperties != null) {
            MessageProperties.HeaderConfig headerConfig = messageProperties.getHeader();
            if (headerConfig.isAutoTraceId()) {
                headers.put("traceId", generateTraceId());
            }
            if (headerConfig.isAutoTimestamp()) {
                headers.put("timestamp", System.currentTimeMillis());
            }
            if (headerConfig.isAutoServiceName()) {
                headers.put("serviceName", getServiceName());
            }
        } else {
            
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());
            headers.put("serviceName", getServiceName());
        }

        return headers;
    }

    








    protected <T> boolean sendMessage(String bindingName, T payload, Map<String, Object> headers) {
        try {
            Message<T> message = new GenericMessage<>(payload, headers);
            String traceId = (String) headers.get("traceId");
            String eventType = (String) headers.get("eventType");

            
            boolean verbose = messageProperties == null || messageProperties.getLog().isVerbose();
            boolean logPayload = messageProperties != null && messageProperties.getLog().isLogPayload();
            boolean logHeaders = messageProperties == null || messageProperties.getLog().isLogHeaders();

            if (verbose) {
                if (logPayload) {
                    String payloadStr = truncatePayload(String.valueOf(payload));
                    

                } else {
                    

                }
                if (logHeaders) {
                    log.debug("D messageebug", headers);
                }
            }

            boolean sent = streamBridge.send(bindingName, message);

            if (sent) {
                if (verbose) {
                    

                }
            } else {
                log.error("??- : {}, : {}, ID: {}",
                        bindingName, eventType, traceId);
            }

            return sent;
        } catch (Exception e) {
            log.error("E messagerror", bindingName, e.getMessage(), e);
            return false;
        }
    }

    


    private String truncatePayload(String payload) {
        if (messageProperties == null) {
            return payload.length() > 1000 ? payload.substring(0, 1000) + "..." : payload;
        }
        int maxLength = messageProperties.getLog().getPayloadMaxLength();
        return payload.length() > maxLength ? payload.substring(0, maxLength) + "..." : payload;
    }

    





    @Deprecated
    protected String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }

    





    protected void logMessageProcessStart(String eventType, String traceId) {
        

    }

    





    protected void logMessageProcessSuccess(String eventType, String traceId) {
        

    }

    






    protected void logMessageProcessError(String eventType, String traceId, String error) {
        log.error("? - : {}, ID: {}, : {}, : {}",
                eventType, traceId, getServiceName(), error);
    }

    






    protected boolean isMessageProcessed(String traceId) {
        
        return false;
    }

    





    protected void markMessageProcessed(String traceId) {
        
        log.debug("?- ID: {}", traceId);
    }
}
