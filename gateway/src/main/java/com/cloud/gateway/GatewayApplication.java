package com.cloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author what's up
 */
@Slf4j
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
})
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.cloud.gateway"
})
public class GatewayApplication {

    public static void main(String[] args) {
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