package com.cloud.stock;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication(
    scanBasePackages = {"com.cloud.stock", "com.cloud.common"}
)
@ComponentScan(
    basePackages = {"com.cloud.stock", "com.cloud.common"}
)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableAsync
@EnableCaching
@EnableFeignClients(basePackages = "com.cloud.api")
@MapperScan("com.cloud.stock.mapper")
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
