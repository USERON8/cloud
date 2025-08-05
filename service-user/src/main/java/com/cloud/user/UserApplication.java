package com.cloud.user;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class UserApplication {
    public static void main(String[] args) {
        // 禁用Nacos的日志配置，避免冲突
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");
        SpringApplication.run(UserApplication.class, args);
    }
}
