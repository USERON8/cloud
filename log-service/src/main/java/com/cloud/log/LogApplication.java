package com.cloud.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * 日志服务启动类
 *
 * @author what's up
 */
@Slf4j
@SpringBootApplication(
    exclude = {
        com.cloud.common.config.RedissonConfig.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = {"com.cloud.log", "com.cloud.common"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            com.cloud.common.config.BaseAsyncConfig.class,
            com.cloud.common.config.MybatisPlusConfig.class
        })
    }
)
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LogApplication {

    public static void main(String[] args) {
        try {
            // 禁用Nacos的日志配置，避免冲突
            System.setProperty("nacos.logging.default.config.enabled", "false");
            System.setProperty("nacos.logging.config", "");
            System.setProperty("nacos.logging.path", "");

            log.info("正在启动日志服务...");
            SpringApplication.run(LogApplication.class, args);
            log.info("日志服务启动完成！");
        } catch (Exception e) {
            log.error("日志服务启动失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}