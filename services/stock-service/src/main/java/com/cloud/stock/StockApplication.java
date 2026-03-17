package com.cloud.stock;

import com.cloud.common.messaging.config.OutboxAutoConfiguration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Stock Service API",
            description = "Stock service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication(scanBasePackages = {"com.cloud.stock", "com.cloud.common"})
@EnableDubbo
@ComponentScan(basePackages = {"com.cloud.stock", "com.cloud.common"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@EnableCaching
@MapperScan({"com.cloud.stock.mapper", "com.cloud.common.messaging.outbox"})
@Import(OutboxAutoConfiguration.class)
public class StockApplication {
  public static void main(String[] args) {

    System.setProperty("nacos.logging.default.config.enabled", "false");
    System.setProperty("nacos.logging.config", "");
    System.setProperty("nacos.logging.path", "");

    SpringApplication.run(StockApplication.class, args);
  }
}
