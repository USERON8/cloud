package com.cloud.user;

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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "User Service API",
            description = "User service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
@EnableCaching
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.cloud.user.mapper")
public class UserApplication implements WebMvcConfigurer {

  public static void main(String[] args) {
    CloudBootstrap.initialize();
    SpringApplication.run(UserApplication.class, args);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
    registry
        .addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
}
