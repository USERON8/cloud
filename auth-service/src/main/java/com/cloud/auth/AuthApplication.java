package com.cloud.auth;

import com.cloud.common.config.MybatisPlusConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * @author what's up
 */
@Slf4j
@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {"com.cloud.auth", "com.cloud.common"},
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                MybatisPlusConfig.class,
                                com.cloud.common.config.RedissonConfig.class,
                                com.cloud.common.config.base.BaseHealthCheckController.class,
                                com.cloud.common.cache.listener.CacheMessageListener.class,
                                com.cloud.common.cache.metrics.CacheMetricsCollector.class,
                                com.cloud.common.monitoring.PerformanceMonitor.class,
                                com.cloud.common.security.RateLimitManager.class
                        }
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.cloud\\.common\\.config\\.HybridCacheConfig"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.cloud\\.common\\.config\\.base\\.example\\..*"
                )
        }
)
@EnableFeignClients(basePackages = "com.cloud.api")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthApplication {

    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        log.info("正在启动认证服务...");
        SpringApplication.run(AuthApplication.class, args);
        log.info("认证服务启动完成！");
    }
}