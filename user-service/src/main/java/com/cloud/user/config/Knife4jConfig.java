package com.cloud.user.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 用户服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "用户服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "用户管理、商家管理、用户地址管理相关的 RESTful API 文档";
    }
}
