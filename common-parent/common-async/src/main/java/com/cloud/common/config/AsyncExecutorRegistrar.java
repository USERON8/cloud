package com.cloud.common.config;

import com.cloud.common.config.properties.AsyncProperties;
import com.cloud.common.config.properties.AsyncProperties.ThreadPoolConfig;
import com.cloud.common.threadpool.FastFailRejectedExecutionHandler;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class AsyncExecutorRegistrar
    implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    AsyncProperties properties =
        Binder.get(environment)
            .bind("app.async", Bindable.of(AsyncProperties.class))
            .orElseGet(AsyncProperties::new);

    if (!properties.isEnabled()) {
      return;
    }

    Map<String, ThreadPoolConfig> executors = properties.getExecutors();
    if (executors == null || executors.isEmpty()) {
      return;
    }

    boolean useTaskDecorator =
        properties.getCommon() != null && properties.getCommon().isTaskDecorator();

    executors.forEach(
        (beanName, config) -> {
          if (beanName == null || beanName.isBlank() || config == null) {
            return;
          }
          if (!config.isEnabled()) {
            return;
          }
          if (registry.containsBeanDefinition(beanName)) {
            return;
          }

          ThreadPoolTaskExecutor executor = buildExecutor(config);
          BeanDefinitionBuilder builder =
              BeanDefinitionBuilder.genericBeanDefinition(
                  ThreadPoolTaskExecutor.class, () -> executor);
          AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
          if (useTaskDecorator) {
            beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
          }
          registry.registerBeanDefinition(beanName, beanDefinition);
        });
  }

  @Override
  public void postProcessBeanFactory(
      org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
    // no-op
  }

  private ThreadPoolTaskExecutor buildExecutor(ThreadPoolConfig config) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, config.getCorePoolSize()));
    executor.setMaxPoolSize(Math.max(1, config.getMaxPoolSize()));
    executor.setQueueCapacity(Math.max(0, config.getQueueCapacity()));
    String prefix = config.getThreadNamePrefix();
    executor.setThreadNamePrefix(prefix == null || prefix.isBlank() ? "async-" : prefix);
    executor.setKeepAliveSeconds(Math.max(1, config.getKeepAliveSeconds()));
    executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeOut());
    executor.setRejectedExecutionHandler(resolveRejectedExecutionHandler(config));
    executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
    executor.setAwaitTerminationSeconds(Math.max(1, config.getAwaitTerminationSeconds()));
    executor.initialize();
    return executor;
  }

  private RejectedExecutionHandler resolveRejectedExecutionHandler(ThreadPoolConfig config) {
    String handlerType = config.getRejectedExecutionHandler();
    if (handlerType == null || handlerType.isBlank()) {
      return new FastFailRejectedExecutionHandler();
    }
    return switch (handlerType.toUpperCase(Locale.ROOT)) {
      case "FAST_FAIL" -> new FastFailRejectedExecutionHandler();
      case "ABORT" -> new ThreadPoolExecutor.AbortPolicy();
      case "DISCARD" -> new ThreadPoolExecutor.DiscardPolicy();
      case "DISCARD_OLDEST" -> new ThreadPoolExecutor.DiscardOldestPolicy();
      case "CALLER_RUNS" -> new ThreadPoolExecutor.CallerRunsPolicy();
      default -> new ThreadPoolExecutor.CallerRunsPolicy();
    };
  }
}
