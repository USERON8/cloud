package com.cloud.payment.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 支付服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "支付服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "支付管理、支付查询、支付流水管理相关的 RESTful API 文档";
    }
}
