package com.cloud.payment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SecurityScheme(
        name = "Authorization",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "Payment Service API",
                description = "Payment service endpoints",
                version = "1.0.0"
        ),
        security = @SecurityRequirement(name = "Authorization")
)
@SpringBootApplication(scanBasePackages = {"com.cloud.payment", "com.cloud.common"})
@EnableDubbo
@EnableDiscoveryClient
@Slf4j
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan({"com.cloud.payment.mapper", "com.cloud.common.messaging.outbox"})
public class PaymentApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        System.out.println("正在启动支付服务...");
        SpringApplication.run(PaymentApplication.class, args);
        System.out.println("支付服务启动完成！");
    }
}
