package com.cloud.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 统一配置类
 * 各服务继承此类并配置相应的服务信息
 *
 * @author what's up
 */
@Configuration
public class BaseKnife4jConfig {

    @Value("${spring.application.name:cloud-service}")
    private String serviceName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 创建 OpenAPI 实例
     * 各服务可以重写此方法自定义API信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(getServiceTitle())
                        .version("1.0.0")
                        .description(getServiceDescription())
                        .contact(new Contact()
                                .name("what's up")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    /**
     * 获取服务标题，子类可重写
     */
    protected String getServiceTitle() {
        return serviceName.toUpperCase() + " API 文档";
    }

    /**
     * 获取服务描述，子类可重写
     */
    protected String getServiceDescription() {
        return serviceName + " 服务 RESTful API 文档";
    }
}
