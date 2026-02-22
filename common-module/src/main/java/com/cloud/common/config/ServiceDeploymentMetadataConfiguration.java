package com.cloud.common.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;




@Slf4j
@Configuration
@ConditionalOnClass(NacosDiscoveryProperties.class)
public class ServiceDeploymentMetadataConfiguration {

    @Bean
    @ConditionalOnBean(NacosDiscoveryProperties.class)
    public ApplicationRunner serviceDeploymentMetadataInitializer(
            NacosDiscoveryProperties nacosDiscoveryProperties,
            @Value("${spring.application.name:unknown-service}") String applicationName,
            @Value("${app.deploy.singleton-services:admin-service,log-service}") String singletonServices,
            @Value("${SERVICE_SINGLETON:}") String singletonOverride,
            @Value("${SERVICE_DEPLOY_MODE:}") String deployModeOverride,
            @Value("${SERVICE_TRAFFIC_ROLE:}") String trafficRoleOverride) {
        return args -> {
            Map<String, String> metadata = nacosDiscoveryProperties.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
                nacosDiscoveryProperties.setMetadata(metadata);
            }

            Set<String> singletonServiceSet = Arrays.stream(singletonServices.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(service -> service.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            boolean singleton = singletonServiceSet.contains(applicationName.toLowerCase(Locale.ROOT));
            if (StringUtils.hasText(singletonOverride)) {
                singleton = Boolean.parseBoolean(singletonOverride);
            }

            String deployMode = StringUtils.hasText(deployModeOverride)
                    ? deployModeOverride
                    : (singleton ? "single" : "multi");

            String trafficRole = StringUtils.hasText(trafficRoleOverride)
                    ? trafficRoleOverride
                    : (singleton ? "management" : "business");

            metadata.put("singleton", String.valueOf(singleton));
            metadata.put("deploy-mode", deployMode);
            metadata.put("traffic-role", trafficRole);
            metadata.putIfAbsent("instance-id", applicationName + ":" + UUID.randomUUID());

            

        };
    }
}
