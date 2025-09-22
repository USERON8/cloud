package com.cloud.order.config;

import com.cloud.common.config.base.BaseMessageConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务消息配置类
 * 继承通用消息配置
 *
 * @author cloud
 * @date 2025/1/15
 */
@Configuration
public class OrderMessageConfig extends BaseMessageConfig {

    @Override
    protected String getServiceName() {
        return "订单服务消息配置已加载";
    }
}
