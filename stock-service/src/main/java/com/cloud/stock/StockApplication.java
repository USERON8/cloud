package com.cloud.stock;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.cloud.stock.mapper")
@EnableFeignClients(basePackages = "com.cloud.api")
public class StockApplication {
    public static void main(String[] args) {
        // 禁用Nacos的日志配置，避免冲突
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动库存服务...");
        SpringApplication.run(StockApplication.class, args);
        log.info("库存服务启动完成！");
    }


}