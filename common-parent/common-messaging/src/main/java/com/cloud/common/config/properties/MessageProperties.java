package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;








@Data
@Component
@ConfigurationProperties(prefix = "app.message")
public class MessageProperties {

    


    private boolean enabled = true;

    


    private int sendRetryTimes = 3;

    


    private long sendTimeout = 3000;

    


    private boolean traceEnabled = true;

    


    private boolean idempotentEnabled = false;

    


    private long idempotentExpireSeconds = 86400;

    


    private HeaderConfig header = new HeaderConfig();

    


    private LogConfig log = new LogConfig();

    @Data
    public static class HeaderConfig {
        


        private boolean autoTraceId = true;

        


        private boolean autoTimestamp = true;

        


        private boolean autoServiceName = true;

        


        private String customPrefix = "";
    }

    @Data
    public static class LogConfig {
        


        private boolean verbose = true;

        


        private boolean logPayload = false;

        


        private boolean logHeaders = true;

        


        private int payloadMaxLength = 1000;
    }
}

