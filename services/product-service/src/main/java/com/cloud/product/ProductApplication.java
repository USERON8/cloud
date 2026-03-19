package com.cloud.product;

import com.cloud.common.boot.CloudBootstrap;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Product Service API",
            description = "Product service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
@EnableCaching
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("com.cloud.product.mapper")
public class ProductApplication {

  public static void main(String[] args) {
    CloudBootstrap.initialize();
    SpringApplication.run(ProductApplication.class, args);
  }
}
