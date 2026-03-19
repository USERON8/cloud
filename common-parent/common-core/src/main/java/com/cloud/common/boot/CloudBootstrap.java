package com.cloud.common.boot;

public final class CloudBootstrap {

  private CloudBootstrap() {}

  public static void initialize() {
    setIfAbsent("nacos.logging.default.config.enabled", "false");
    setIfAbsent("nacos.logging.config", "");
    setIfAbsent("nacos.logging.path", "");
  }

  private static void setIfAbsent(String key, String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}
