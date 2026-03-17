package com.cloud.order;

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
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Order Service API",
            description = "Order service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication(scanBasePackages = {"com.cloud.order", "com.cloud.common"})
@EnableDubbo
@EnableDiscoveryClient
@EnableScheduling
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan({"com.cloud.order.mapper", "com.cloud.common.messaging.outbox"})
@Import(OutboxAutoConfiguration.class)
public class OrderApplication {
  public static void main(String[] args) {
    System.setProperty("nacos.logging.default.config.enabled", "false");
    System.setProperty("nacos.logging.config", "");
    System.setProperty("nacos.logging.path", "");

    SpringApplication.run(OrderApplication.class, args);
  }
}
