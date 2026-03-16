package com.cloud.gateway.cache;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.search.fallback.cache")
public class SearchFallbackCacheProperties {

  private boolean enabled = true;
  private long maxSize = 1000;
  private long minTtlMs = 500;
  private long searchTtlMs = 3000;
  private long smartSearchTtlMs = 3000;
  private long suggestionsTtlMs = 10000;
  private Map<String, Long> routeTtlMs = new HashMap<>();
  private Map<String, Long> paramTtlMs = new HashMap<>();
}
