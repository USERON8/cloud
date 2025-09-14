package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关 API 文档聚合配置
 * 使用 SpringDoc 聚合所有微服务的 API 文档
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class GatewayKnife4jConfig {

    /**
     * 配置聚合的 API 分组
     */
    @Bean
    public List<GroupedOpenApi> apis() {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        // 认证服务
        groups.add(GroupedOpenApi.builder()
                .group("认证服务")
                .pathsToMatch("/auth-service/**")
                .build());
        
        // 用户服务
        groups.add(GroupedOpenApi.builder()
                .group("用户服务")
                .pathsToMatch("/user-service/**")
                .build());
        
        // 商品服务
        groups.add(GroupedOpenApi.builder()
                .group("商品服务")
                .pathsToMatch("/product-service/**")
                .build());
        
        // 订单服务
        groups.add(GroupedOpenApi.builder()
                .group("订单服务")
                .pathsToMatch("/order-service/**")
                .build());
        
        // 支付服务
        groups.add(GroupedOpenApi.builder()
                .group("支付服务")
                .pathsToMatch("/payment-service/**")
                .build());
        
        // 库存服务
        groups.add(GroupedOpenApi.builder()
                .group("库存服务")
                .pathsToMatch("/stock-service/**")
                .build());
        
        log.info("配置了 {} 个服务的 API 文档分组", groups.size());
        return groups;
    }
}
