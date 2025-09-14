package com.cloud.stock.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 库存服务统一配置类
 *
 * @author what's up
 */
@Configuration
public class StockServiceConfig {
    /**
     * 自定义 Knife4j 配置，继承基础配置
     */
    @Configuration
    public static class StockKnife4jConfig extends BaseKnife4jConfig {
        @Override
        protected String getServiceTitle() {
            return "库存服务 API 文档";
        }

        @Override
        protected String getServiceDescription() {
            return "Cloud微服务库存管理API接口文档";
        }
    }
}
