package com.cloud.common.config;

import com.cloud.common.config.properties.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobExecutorConfig {

  @Bean
  public XxlJobSpringExecutor xxlJobSpringExecutor(
      XxlJobProperties properties, Environment environment) {
    XxlJobProperties.Executor executorProperties = properties.getExecutor();
    String appName =
        firstNonBlank(
            executorProperties.getAppname(), environment.getProperty("spring.application.name"));
    if (appName == null) {
      throw new IllegalStateException(
          "xxl.job.executor.appname or spring.application.name must be configured");
    }

    XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
    executor.setAdminAddresses(properties.getAdminAddresses());
    executor.setAccessToken(properties.getAccessToken());
    executor.setAppname(appName);
    executor.setAddress(executorProperties.getAddress());
    executor.setIp(executorProperties.getIp());
    if (executorProperties.getPort() > 0) {
      executor.setPort(executorProperties.getPort());
    }
    executor.setLogPath(firstNonBlank(executorProperties.getLogPath(), "logs/xxl-job"));
    executor.setLogRetentionDays(Math.max(1, executorProperties.getLogRetentionDays()));

    log.info(
        "XXL-JOB executor enabled: appName={}, adminAddresses={}, port={}",
        appName,
        properties.getAdminAddresses(),
        executorProperties.getPort());
    return executor;
  }

  private String firstNonBlank(String primary, String fallback) {
    if (primary != null && !primary.isBlank()) {
      return primary.trim();
    }
    if (fallback != null && !fallback.isBlank()) {
      return fallback.trim();
    }
    return null;
  }
}
