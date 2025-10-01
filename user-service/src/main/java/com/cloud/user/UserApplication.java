package com.cloud.user;

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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author what's up
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@Slf4j
@EnableFeignClients(basePackages = "com.cloud.api")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(
    basePackages = {"com.cloud.user", "com.cloud.common"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.cloud.common.cache.listener.CacheMessageListener.class,
                com.cloud.common.cache.metrics.CacheMetricsCollector.class,
                com.cloud.common.monitoring.PerformanceMonitor.class,
                com.cloud.common.security.RateLimitManager.class,
                com.cloud.common.config.SecurityConfig.class
            }
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.cloud\\.common\\.config\\.base\\.example\\..*"
        )
    }
)
@MapperScan("com.cloud.user.mapper")

public class UserApplication implements WebMvcConfigurer {
    public static void main(String[] args) {
        try {
            // 禁用Nacos的日志配置，避免冲突
            System.setProperty("nacos.logging.default.config.enabled", "false");
            System.setProperty("nacos.logging.config", "");
            System.setProperty("nacos.logging.path", "");


            log.info("正在启动用户服务...");
            SpringApplication.run(UserApplication.class, args);
            log.info("用户服务启动完成！");
        } catch (Exception e) {
            log.error("用户服务启动失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 配置Knife4j静态资源访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}