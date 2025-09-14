package com.cloud.auth.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 认证服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "认证服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "用户认证、OAuth2 授权、登录注册相关的 RESTful API 文档";
    }
}
