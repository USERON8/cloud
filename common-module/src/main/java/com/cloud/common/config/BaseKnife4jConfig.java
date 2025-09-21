package com.cloud.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j基础配置类
 *
 * @author cloud
 * @date 2024-01-20
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)
public abstract class BaseKnife4jConfig {

    /**
     * 创建基础OpenAPI配置
     *
     * @param title       API标题
     * @param description API描述
     * @param version     API版本
     * @return OpenAPI配置
     */
    protected OpenAPI createOpenAPI(String title, String description, String version) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact()
                                .name("Cloud Team")
                                .email("cloud@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境"),
                        new Server().url("https://api.example.com").description("生产环境")
                ));
    }

    /**
     * 获取服务标题
     *
     * @return 服务标题
     */
    protected abstract String getServiceTitle();

    /**
     * 获取服务描述
     *
     * @return 服务描述
     */
    protected abstract String getServiceDescription();

    /**
     * 获取服务版本
     *
     * @return 服务版本
     */
    protected String getServiceVersion() {
        return "1.0.0";
    }

    /**
     * 默认OpenAPI配置
     *
     * @return OpenAPI配置
     */
    @Bean
    public OpenAPI defaultOpenAPI() {
        return createOpenAPI(
                getServiceTitle(),
                getServiceDescription(),
                getServiceVersion()
        );
    }
}
