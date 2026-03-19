package com.cloud.common.messaging.config;

import com.cloud.common.messaging.outbox.OutboxEventMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@AutoConfiguration(before = OutboxAutoConfiguration.class)
@ConditionalOnClass({SqlSessionFactory.class, MapperFactoryBean.class, OutboxEventMapper.class})
@ConditionalOnBean(SqlSessionFactory.class)
@MapperScan(basePackageClasses = OutboxEventMapper.class)
public class OutboxMapperAutoConfiguration {}
