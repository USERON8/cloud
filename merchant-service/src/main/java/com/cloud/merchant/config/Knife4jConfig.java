package com.cloud.merchant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置类
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud商家服务API文档")
                        .version("1.0")
                        .description("Cloud微服务商家API接口文档")
                        .contact(new Contact()
                                .name("cloud shop")
                                .url("https://github.com/USERON8/cloud")
                                .email("sltt_0212@qq.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }
}