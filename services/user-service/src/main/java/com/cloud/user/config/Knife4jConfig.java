package com.cloud.user.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "User Service API Documentation";
    }

    @Override
    protected String getServiceDescription() {
        return "RESTful APIs for user management, merchant management, and user address management.";
    }
}