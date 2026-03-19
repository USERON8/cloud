package com.cloud.auth;

import com.cloud.common.boot.CloudBootstrap;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Auth Service API",
            description = "Authentication service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication
@EnableDubbo
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthApplication {

  public static void main(String[] args) {
    CloudBootstrap.initialize();
    SpringApplication.run(AuthApplication.class, args);
  }
}
