package com.cloud.order.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "订单服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "订单管理、订单查询、订单状态管理相关的 RESTful API 文档";
    }
}
