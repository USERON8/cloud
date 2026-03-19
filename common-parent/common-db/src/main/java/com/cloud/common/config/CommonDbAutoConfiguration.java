package com.cloud.common.config;

import com.cloud.common.task.SeataUndoLogCleanupXxlJob;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
  RedisConfig.class,
  RedissonClientConfiguration.class,
  MybatisPlusConfig.class,
  SeataUndoLogCleanupXxlJob.class
})
public class CommonDbAutoConfiguration {}
