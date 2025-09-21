package com.cloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author what's up
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        // Nacos日志配置优化
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动网关服务...");
        SpringApplication.run(GatewayApplication.class, args);
        log.info("网关服务启动完成！");
        log.info("""
                
                 -------------------------------------------------
                     Application is running! Access URLs:
                     Local:    http://localhost
                 -------------------------------------------------
                
                """);

    }
}