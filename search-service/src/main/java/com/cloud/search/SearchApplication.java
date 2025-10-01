package com.cloud.search;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(
    scanBasePackages = {"com.cloud.search", "com.cloud.common"}
)
@ComponentScan(
    basePackages = {"com.cloud.search", "com.cloud.common"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.cloud.common.config.SecurityConfig.class
            }
        )
    }
)
@EnableDiscoveryClient
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SearchApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动搜索服务...");
        SpringApplication.run(SearchApplication.class, args);
        log.info("搜索服务启动完成！");
    }
}
