package com.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        // 禁用Nacos的日志配置，避免冲突
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动网关服务...");
        SpringApplication.run(GatewayApplication.class, args);
        log.info("网关服务启动完成！");
    }
}
