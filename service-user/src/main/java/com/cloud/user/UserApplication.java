package com.cloud.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
@Slf4j
@MapperScan("com.cloud.user.mapper")
public class UserApplication {
    public static void main(String[] args) {
        try {
            // 禁用Nacos的日志配置，避免冲突
            System.setProperty("nacos.logging.default.config.enabled", "false");
            System.setProperty("nacos.logging.config", "");
            System.setProperty("nacos.logging.path", "");

            log.info("正在启动用户服务...");
            SpringApplication.run(UserApplication.class, args);
            log.info("用户服务启动完成！");
        } catch (Exception e) {
            log.error("用户服务启动失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}