package com.cloud.common.config;

import com.cloud.common.config.properties.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(prefix = "app.xxl.job", name = "enabled", havingValue = "true")
public class XxlJobAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAppname(properties.getAppname());
        executor.setIp(properties.getIp());
        executor.setPort(properties.getPort());
        executor.setAccessToken(properties.getAccessToken());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());
        return executor;
    }
}
