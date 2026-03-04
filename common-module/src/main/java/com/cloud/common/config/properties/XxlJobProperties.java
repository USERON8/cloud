package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.xxl.job")
public class XxlJobProperties {

    private boolean enabled = false;
    private String adminAddresses = "http://127.0.0.1:18080/xxl-job-admin";
    private String appname = "cloud-executor";
    private String ip = "";
    private int port = 9999;
    private String accessToken = "";
    private String logPath = "./logs/xxl-job";
    private int logRetentionDays = 30;
}
