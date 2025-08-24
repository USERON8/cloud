package com.cloud.common.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 统一日志配置规范
 * 实现控制台和文件的双输出，按天滚动日志文件
 * 日志格式统一，便于日志收集和分析
 */
@Configuration
public class BaseLogConfig {

    private static final Logger logger = LoggerFactory.getLogger(BaseLogConfig.class);

    /**
     * 配置控制台日志输出
     */
    @Bean
    public ConsoleAppender<ILoggingEvent> consoleAppender() {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(getLoggerContext());
        
        // 设置日志级别过滤器
        ThresholdFilter thresholdFilter = new ThresholdFilter();
        thresholdFilter.setLevel("INFO");
        thresholdFilter.start();
        consoleAppender.addFilter(thresholdFilter);
        
        // 设置日志格式
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(getLoggerContext());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        consoleAppender.setEncoder(encoder);
        
        consoleAppender.start();
        return consoleAppender;
    }

    /**
     * 配置文件日志输出
     */
    @Bean
    public RollingFileAppender<ILoggingEvent> fileAppender() {
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(getLoggerContext());
        fileAppender.setFile("logs/application.log");
        
        // 设置日志级别过滤器
        ThresholdFilter thresholdFilter = new ThresholdFilter();
        thresholdFilter.setLevel("INFO");
        thresholdFilter.start();
        fileAppender.addFilter(thresholdFilter);
        
        // 设置日志格式
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(getLoggerContext());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        fileAppender.setEncoder(encoder);
        
        // 设置滚动策略
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(getLoggerContext());
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern("logs/application.%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("100MB"));
        rollingPolicy.setMaxHistory(30); // 保留30天日志
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("10GB"));
        rollingPolicy.start();
        
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();
        
        return fileAppender;
    }

    /**
     * 获取LoggerContext
     */
    private LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }
}