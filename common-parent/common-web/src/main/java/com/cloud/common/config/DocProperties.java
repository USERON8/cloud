package com.cloud.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.doc")
public class DocProperties {

  private boolean enabled = true;

  private String title;

  private String description;

  private String version = "1.0.0";
}

