package com.cloud.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4jBulkheadProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerResilience4JFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

@Configuration(proxyBeanMethods = false)
public class GatewayCircuitBreakerConfig {

  @Bean
  @ConditionalOnMissingBean(ReactiveResilience4JCircuitBreakerFactory.class)
  public ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory(
      ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider,
      ObjectProvider<TimeLimiterRegistry> timeLimiterRegistryProvider,
      ObjectProvider<ReactiveResilience4jBulkheadProvider> bulkheadProvider,
      ObjectProvider<Resilience4JConfigurationProperties> configurationPropertiesProvider) {
    CircuitBreakerRegistry circuitBreakerRegistry =
        circuitBreakerRegistryProvider.getIfAvailable(CircuitBreakerRegistry::ofDefaults);
    TimeLimiterRegistry timeLimiterRegistry =
        timeLimiterRegistryProvider.getIfAvailable(TimeLimiterRegistry::ofDefaults);
    ReactiveResilience4jBulkheadProvider effectiveBulkheadProvider =
        bulkheadProvider.getIfAvailable();
    Resilience4JConfigurationProperties configurationProperties =
        configurationPropertiesProvider.getIfAvailable(Resilience4JConfigurationProperties::new);
    if (effectiveBulkheadProvider != null) {
      return new ReactiveResilience4JCircuitBreakerFactory(
          circuitBreakerRegistry,
          timeLimiterRegistry,
          effectiveBulkheadProvider,
          configurationProperties);
    }
    return new ReactiveResilience4JCircuitBreakerFactory(
        circuitBreakerRegistry, timeLimiterRegistry, configurationProperties);
  }

  @Bean
  @ConditionalOnMissingBean(SpringCloudCircuitBreakerResilience4JFilterFactory.class)
  public SpringCloudCircuitBreakerResilience4JFilterFactory
      springCloudCircuitBreakerResilience4JFilterFactory(
          ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory,
          ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
    return new SpringCloudCircuitBreakerResilience4JFilterFactory(
        circuitBreakerFactory, dispatcherHandlerProvider);
  }
}
