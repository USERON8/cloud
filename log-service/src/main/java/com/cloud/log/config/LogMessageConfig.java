package com.cloud.log.config;

import com.cloud.common.config.base.BaseMessageConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 日志服务消息配置类
 * 继承通用消息配置
 *
 * @author cloud
 * @date 2025/1/15
 */
@Configuration
public class LogMessageConfig extends BaseMessageConfig {

    @Override
    protected String getServiceName() {
        return "日志服务消息配置已加载 - RocketMQ消费者集成";
    }
}
