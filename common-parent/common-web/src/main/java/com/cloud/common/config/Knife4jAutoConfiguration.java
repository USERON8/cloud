package com.cloud.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(DocProperties.class)
@ConditionalOnProperty(name = "app.doc.enabled", havingValue = "true", matchIfMissing = true)
public class Knife4jAutoConfiguration extends BaseKnife4jConfig {

  private final DocProperties docProperties;
  private final Environment environment;

  public Knife4jAutoConfiguration(DocProperties docProperties, Environment environment) {
    this.docProperties = docProperties;
    this.environment = environment;
  }

  @Override
  protected String getServiceTitle() {
    if (StringUtils.hasText(docProperties.getTitle())) {
      return docProperties.getTitle();
    }
    return environment.getProperty("spring.application.name", "service");
  }

  @Override
  protected String getServiceDescription() {
    if (StringUtils.hasText(docProperties.getDescription())) {
      return docProperties.getDescription();
    }
    return "Service API";
  }

  @Override
  protected String getServiceVersion() {
    if (StringUtils.hasText(docProperties.getVersion())) {
      return docProperties.getVersion();
    }
    return "1.0.0";
  }
}

