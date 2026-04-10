package com.cloud.common.config.actuator;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ActuatorConfig {

  @Bean
  HealthIndicator cloudServiceHealthIndicator(Environment environment) {
    return new HealthIndicator() {
      @Override
      public Health health() {
        try {
          String serviceName =
              environment.getProperty("spring.application.name", "unknown-service");
          String profile = String.join(",", environment.getActiveProfiles());
          String serverPort = environment.getProperty("server.port", "unknown");
          boolean isHealthy = checkServiceHealth();
          Health.Builder builder = isHealthy ? Health.up() : Health.down();

          return builder
              .withDetail("service", serviceName)
              .withDetail("profile", profile)
              .withDetail("port", serverPort)
              .withDetail("status", isHealthy ? "UP" : "DOWN")
              .withDetail("timestamp", System.currentTimeMillis())
              .build();
        } catch (Exception e) {
          return Health.down()
              .withDetail("error", e.getMessage())
              .withDetail("timestamp", System.currentTimeMillis())
              .build();
        }
      }

      private boolean checkServiceHealth() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;
        return memoryUsage < 0.9;
      }
    };
  }

  @Bean
  MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
    return registry -> {
      String serviceName = environment.getProperty("spring.application.name", "unknown-service");
      String profile = String.join(",", environment.getActiveProfiles());

      registry
          .config()
          .commonTags(
              "service", serviceName,
              "profile", profile,
              "platform", "cloud-microservices");
    };
  }
}
