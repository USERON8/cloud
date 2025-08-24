package com.cloud.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer监控配置基类
 * 提供统一的监控指标配置
 */
@Configuration
public class BaseMetricsConfig {

    /**
     * 自定义MeterRegistry
     * @return MeterRegistryCustomizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "cloud-app")
                .commonTags("version", "1.0.0");
    }

    /**
     * 创建请求计数器
     * @param meterRegistry MeterRegistry
     * @return Counter
     */
    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("http.requests")
                .description("HTTP request count")
                .register(meterRegistry);
    }

    /**
     * 创建请求处理时间计量器
     * @param meterRegistry MeterRegistry
     * @return Timer
     */
    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("http.request.duration")
                .description("HTTP request duration")
                .register(meterRegistry);
    }
}