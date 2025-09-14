package com.cloud.product.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 商品服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "商品服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "商品管理、分类管理、店铺管理相关的 RESTful API 文档";
    }
}
