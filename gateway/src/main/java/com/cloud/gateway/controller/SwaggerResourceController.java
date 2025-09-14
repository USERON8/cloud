package com.cloud.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Swagger 资源聚合控制器
 * 为网关提供聚合的 API 文档资源
 *
 * @author what's up
 */
@Slf4j
@RestController
public class SwaggerResourceController {

    @Value("${server.port:80}")
    private String serverPort;

    /**
     * 获取所有微服务的 Swagger 资源配置
     */
    @GetMapping("/swagger-resources")
    public Mono<ResponseEntity<List<SwaggerResource>>> swaggerResources() {
        List<SwaggerResource> resources = new ArrayList<>();
        
        // 手动配置各个服务的 Swagger 资源
        resources.add(createSwaggerResource("认证服务", "/auth-service/v3/api-docs"));
        resources.add(createSwaggerResource("用户服务", "/user-service/v3/api-docs"));
        resources.add(createSwaggerResource("商品服务", "/product-service/v3/api-docs"));
        resources.add(createSwaggerResource("订单服务", "/order-service/v3/api-docs"));
        resources.add(createSwaggerResource("支付服务", "/payment-service/v3/api-docs"));
        resources.add(createSwaggerResource("库存服务", "/stock-service/v3/api-docs"));
        
        log.info("聚合了 {} 个服务的 Swagger 资源", resources.size());
        return Mono.just(ResponseEntity.ok(resources));
    }

    /**
     * 获取 Swagger UI 配置
     */
    @GetMapping("/swagger-resources/configuration/ui")
    public Mono<ResponseEntity<Map<String, Object>>> uiConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("deepLinking", true);
        config.put("displayOperationId", false);
        config.put("defaultModelsExpandDepth", 1);
        config.put("defaultModelExpandDepth", 1);
        config.put("defaultModelRendering", "example");
        config.put("displayRequestDuration", false);
        config.put("docExpansion", "none");
        config.put("filter", false);
        config.put("maxDisplayedTags", null);
        config.put("operationsSorter", null);
        config.put("showExtensions", false);
        config.put("tagsSorter", null);
        config.put("validatorUrl", "");
        
        return Mono.just(ResponseEntity.ok(config));
    }

    /**
     * 获取 Swagger Security 配置
     */
    @GetMapping("/swagger-resources/configuration/security")
    public Mono<ResponseEntity<Map<String, Object>>> securityConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("clientId", "");
        config.put("clientSecret", "");
        config.put("realm", "");
        config.put("appName", "Gateway API 文档");
        config.put("scopeSeparator", " ");
        config.put("useBasicAuthenticationWithAccessCodeGrant", false);
        config.put("usePkceWithAuthorizationCodeGrant", true);
        
        return Mono.just(ResponseEntity.ok(config));
    }

    /**
     * 创建 Swagger 资源对象
     */
    private SwaggerResource createSwaggerResource(String name, String location) {
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(name);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion("3.0.3");
        return swaggerResource;
    }

    /**
     * Swagger 资源实体类
     */
    public static class SwaggerResource {
        private String name;
        private String location;
        private String swaggerVersion;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getSwaggerVersion() {
            return swaggerVersion;
        }

        public void setSwaggerVersion(String swaggerVersion) {
            this.swaggerVersion = swaggerVersion;
        }
    }
}
