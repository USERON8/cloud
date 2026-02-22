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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
@EnableCaching
@EnableFeignClients(basePackages = "com.cloud.api")
@MapperScan("com.cloud.stock.mapper")
public class StockApplication {
    public static void main(String[] args) {
        
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        
        SpringApplication.run(StockApplication.class, args);
    }
}
