package com.cloud.order;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {"com.cloud.order", "com.cloud.common"}
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cloud.api")
@EnableAsync
@EnableScheduling
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.cloud.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动订单服务...");
        SpringApplication.run(OrderApplication.class, args);
        log.info("订单服务启动完成！");
    }
}