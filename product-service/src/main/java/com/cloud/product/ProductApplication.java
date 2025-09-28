package com.cloud.product;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;
import com.cloud.common.config.RedissonConfig;
import com.cloud.common.config.base.BaseHealthCheckController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@Slf4j
@EnableFeignClients(basePackages = "com.cloud.api")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(
    basePackages = {"com.cloud.product", "com.cloud.common"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                BaseHealthCheckController.class,
                com.cloud.common.cache.listener.CacheMessageListener.class,
                com.cloud.common.cache.metrics.CacheMetricsCollector.class,
                com.cloud.common.monitoring.PerformanceMonitor.class,
                com.cloud.common.security.RateLimitManager.class
            }
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.cloud\\.common\\.config\\.base\\.example\\..*"
        )
    }
)
@MapperScan("com.cloud.product.mapper")
public class ProductApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动商品服务...");
        SpringApplication.run(ProductApplication.class, args);
        log.info("商品服务启动完成！");
    }
}