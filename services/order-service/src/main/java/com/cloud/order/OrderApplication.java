package com.cloud.order;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
        scanBasePackages = {"com.cloud.order", "com.cloud.common"}
)
@EnableDubbo
@EnableDiscoveryClient
@EnableAsync
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.cloud.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        
        SpringApplication.run(OrderApplication.class, args);
        
    }
}
