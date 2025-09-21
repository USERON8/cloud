package com.cloud.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validator配置类
 * 解决多个Validator Bean冲突问题
 * 
 * @author Cloud
 */
@Configuration
public class ValidatorConfig {

    /**
     * 创建主Validator Bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "defaultValidator")
    public Validator primaryValidator() {
        return new LocalValidatorFactoryBean();
    }
}
