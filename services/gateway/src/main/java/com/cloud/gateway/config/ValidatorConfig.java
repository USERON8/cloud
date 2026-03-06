package com.cloud.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;







@Configuration
public class ValidatorConfig {

    


    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "defaultValidator")
    public Validator primaryValidator() {
        return new LocalValidatorFactoryBean();
    }
}
