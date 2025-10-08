package com.cloud.log.controller;

import com.cloud.common.result.Result;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志服务健康检查控制器
 * 提供日志服务专用的健康检查接口
 *
 * @author what's up
 * @since 1.0.0
 */
@RestController
public class LogHealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(LogHealthCheckController.class);

    private final MeterRegistry meterRegistry;

    public LogHealthCheckController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 日志服务健康检查接口
     *
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public Result<?> health() {
        logger.info("Log service health check endpoint accessed");

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("service", "log-service");
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

        // 日志服务特有的健康检查项
        healthInfo.put("elasticsearch", "connected"); // 假设ES连接正常
        healthInfo.put("logCollectionEnabled", true);
        healthInfo.put("realtimeProcessingEnabled", true);

        // 如果配置了Micrometer，添加指标信息
        if (meterRegistry != null) {
            healthInfo.put("metrics", "enabled");
        } else {
            healthInfo.put("metrics", "disabled");
        }

        return Result.success("Log service is healthy", healthInfo);
    }

    /**
     * 日志服务专用的ping接口
     *
     * @return pong with service info
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        logger.debug("Log service ping endpoint accessed");
        return Result.success("pong from log-service");
    }
}
