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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@Slf4j
@EnableFeignClients(basePackages = "com.cloud.api")
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
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
                                com.cloud.common.security.RateLimitManager.class
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
            System.setProperty("nacos.logging.default.config.enabled", "false");
            System.setProperty("nacos.logging.config", "");
            System.setProperty("nacos.logging.path", "");
            SpringApplication.run(UserApplication.class, args);
        } catch (Exception e) {
            log.error("User service startup failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
