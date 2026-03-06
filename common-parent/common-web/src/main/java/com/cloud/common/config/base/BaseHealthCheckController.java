package com.cloud.common.config.base;

import com.cloud.common.result.Result;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;






@RequiredArgsConstructor
public class BaseHealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(BaseHealthCheckController.class);


    private final MeterRegistry meterRegistry;

    




    @GetMapping("/health")
    public Result<?> health() {
        logger.info("Health check endpoint accessed");

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());

        try {
            InetAddress ip = InetAddress.getLocalHost();
            healthInfo.put("host", ip.getHostName());
            healthInfo.put("ip", ip.getHostAddress());
        } catch (UnknownHostException e) {
            logger.warn("Failed to get host information", e);
            healthInfo.put("host", "unknown");
            healthInfo.put("ip", "unknown");
        }

        
        if (meterRegistry != null) {
            healthInfo.put("metrics", "enabled");
        } else {
            healthInfo.put("metrics", "disabled");
        }

        return Result.success(healthInfo.toString(), "Service is healthy");
    }

    




    @GetMapping("/ping")
    public Result<String> ping() {
        logger.debug("Ping endpoint accessed");
        return Result.success("pong");
    }
}
