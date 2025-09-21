package com.cloud.gateway.config;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway服务发现配置
 * 
 * @author Cloud
 */
@Configuration
public class GatewayDiscoveryConfig {

    /**
     * 创建服务发现路由定位器Bean
     * 解决Knife4j Gateway Starter需要DiscoveryClientRouteDefinitionLocator的问题
     */
    @Bean
    public DiscoveryClientRouteDefinitionLocator discoveryClientRouteDefinitionLocator(
            ReactiveDiscoveryClient reactiveDiscoveryClient, 
            DiscoveryLocatorProperties properties) {
        return new DiscoveryClientRouteDefinitionLocator(reactiveDiscoveryClient, properties);
    }
    
    /**
     * 服务发现定位器属性
     */
    @Bean
    public DiscoveryLocatorProperties discoveryLocatorProperties() {
        DiscoveryLocatorProperties properties = new DiscoveryLocatorProperties();
        properties.setEnabled(true);
        properties.setLowerCaseServiceId(true);
        return properties;
    }
}
