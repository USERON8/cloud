package com.cloud.user.config;

import com.cloud.common.config.BaseMyBatisPlusConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.cloud.user.mapper")
public class MyBatisConfig extends BaseMyBatisPlusConfig {


}