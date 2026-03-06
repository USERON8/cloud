package com.cloud.stock.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "Stock Service API Documentation";
    }

    @Override
    protected String getServiceDescription() {
        return "RESTful APIs for stock management, stock in/out, reservation, and stock counting.";
    }
}
