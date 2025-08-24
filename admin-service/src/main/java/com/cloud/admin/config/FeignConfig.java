package com.cloud.admin.config;

import feign.Logger;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign配置类
 */
@Configuration
@EnableFeignClients(basePackages = "com.cloud.api")
public class FeignConfig {
    
    /**
     * 配置Feign日志级别
     *
     * @return Logger.Level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}