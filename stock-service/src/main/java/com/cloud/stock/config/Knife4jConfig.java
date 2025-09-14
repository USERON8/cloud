package com.cloud.stock.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 库存服务 Knife4j 配置
 *
 * @author what's up
 */
@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "库存服务 API 文档";
    }

    @Override
    protected String getServiceDescription() {
        return "库存管理、库存查询、库存出入库操作相关的 RESTful API 文档";
    }
}
