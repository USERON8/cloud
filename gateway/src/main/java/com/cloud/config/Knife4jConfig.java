package com.cloud.config;

import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class Knife4jConfig {

    private final RouteDefinitionLocator routeDefinitionLocator;

    public Knife4jConfig(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @Bean
    @Lazy(false)
    public List<org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl> swaggerUrls() {
        List<org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl> urls = new ArrayList<>();
        
        // 获取所有路由定义
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        
        if (routes != null) {
            routes.forEach(routeDefinition -> {
                String id = routeDefinition.getId();
                String serviceName = getServiceNameFromRouteId(id);
                String title = getServiceTitle(routeDefinition);
                
                // 为所有服务创建文档聚合
                urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                    title, "/" + serviceName + "/v3/api-docs", serviceName));
            });
        }
        
        // 添加网关自身的文档
        urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
            "网关服务", "/v3/api-docs", "gateway"));
            
        return urls;
    }
    
    /**
     * 根据路由ID获取服务名称
     * @param routeId 路由ID
     * @return 服务名称
     */
    private String getServiceNameFromRouteId(String routeId) {
        if (routeId.endsWith("-route")) {
            return routeId.substring(0, routeId.length() - 6) + "-service";
        }
        return routeId;
    }
    
    /**
     * 从路由定义中获取服务标题
     * @param routeDefinition 路由定义
     * @return 服务标题
     */
    private String getServiceTitle(RouteDefinition routeDefinition) {
        // 从metadata中获取标题信息
        Map<String, Object> metadata = routeDefinition.getMetadata();
        if (metadata != null && metadata.containsKey("swagger-info")) {
            Object swaggerInfo = metadata.get("swagger-info");
            if (swaggerInfo instanceof Map) {
                Map<String, Object> info = (Map<String, Object>) swaggerInfo;
                if (info.containsKey("title")) {
                    return (String) info.get("title");
                }
            }
        }
        
        // 如果没有配置标题，则使用路由ID生成
        String id = routeDefinition.getId();
        if (id.endsWith("-route")) {
            return id.substring(0, 1).toUpperCase() + id.substring(1, id.length() - 6) + "服务";
        }
        return id;
    }
}