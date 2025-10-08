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

/**
 * 健康检查控制器基类
 * 提供统一的健康检查接口模板
 * 注意：此类仅用于继承，不作为Spring Controller Bean
 */
@RequiredArgsConstructor
public class BaseHealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(BaseHealthCheckController.class);


    private final MeterRegistry meterRegistry;

    /**
     * 健康检查接口
     *
     * @return 健康状态信息
     */
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

        // 如果配置了Micrometer，添加指标信息
        if (meterRegistry != null) {
            healthInfo.put("metrics", "enabled");
        } else {
            healthInfo.put("metrics", "disabled");
        }

        return Result.success(healthInfo.toString(), "Service is healthy");
    }

    /**
     * 简单的ping接口
     *
     * @return pong
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        logger.debug("Ping endpoint accessed");
        return Result.success("pong");
    }
}