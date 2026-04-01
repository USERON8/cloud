package com.cloud.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({RedisConfig.class, RedissonClientConfiguration.class, MybatisPlusConfig.class})
public class CommonDbAutoConfiguration {}
