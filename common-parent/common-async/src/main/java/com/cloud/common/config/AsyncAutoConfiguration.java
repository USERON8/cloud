package com.cloud.common.config;

import com.cloud.common.config.properties.AsyncProperties;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncAutoConfiguration implements AsyncConfigurer, ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Bean
  public static AsyncExecutorRegistrar asyncExecutorRegistrar() {
    return new AsyncExecutorRegistrar();
  }

  @Override
  public Executor getAsyncExecutor() {
    String beanName =
        Optional.ofNullable(
                applicationContext.getEnvironment().getProperty("app.async.default-executor"))
            .map(String::trim)
            .orElse("");
    if (StringUtils.hasText(beanName) && applicationContext.containsBean(beanName)) {
      return applicationContext.getBean(beanName, Executor.class);
    }
    if (applicationContext.containsBean("defaultAsyncExecutor")) {
      return applicationContext.getBean("defaultAsyncExecutor", Executor.class);
    }
    return null;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}

