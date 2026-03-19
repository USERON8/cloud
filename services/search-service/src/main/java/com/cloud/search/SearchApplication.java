package com.cloud.search;

import com.cloud.common.boot.CloudBootstrap;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SecurityScheme(
    name = "Authorization",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Search Service API",
            description = "Search service endpoints",
            version = "1.0.0"),
    security = @SecurityRequirement(name = "Authorization"))
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class})
@EnableDiscoveryClient
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableScheduling
public class SearchApplication {

  public static void main(String[] args) {
    CloudBootstrap.initialize();
    SpringApplication.run(SearchApplication.class, args);
  }
}
