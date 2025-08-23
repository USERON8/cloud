package com.cloud.admin;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cloud.api")
@Slf4j
@MapperScan("com.cloud.admin.mapper")
public class AdminApplication {
    public static void main(String[] args) {
        try {
            // 禁用Nacos的日志配置，避免冲突
            System.setProperty("nacos.logging.default.config.enabled", "false");
            System.setProperty("nacos.logging.config", "");
            System.setProperty("nacos.logging.path", "");

            log.info("正在启动管理员服务...");
            SpringApplication.run(AdminApplication.class, args);
            log.info("管理员服务启动完成！");
        } catch (Exception e) {
            log.error("管理员服务启动失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}