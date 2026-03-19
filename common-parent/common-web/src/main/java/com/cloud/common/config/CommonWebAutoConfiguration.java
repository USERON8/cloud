package com.cloud.common.config;

import com.cloud.common.aop.ServiceExceptionAspect;
import com.cloud.common.config.actuator.ActuatorConfig;
import com.cloud.common.exception.ExceptionReporter;
import com.cloud.common.exception.GlobalExceptionHandler;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.common.web.TraceHeaderFilter;
import com.cloud.common.web.TraceResponseAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
  ActuatorConfig.class,
  ResponseAdvice.class,
  GlobalExceptionHandler.class,
  TraceResponseAdvice.class,
  TraceHeaderFilter.class,
  ServiceExceptionAspect.class,
  ExceptionReporter.class,
  TradeMetrics.class,
  XxlJobExecutorConfig.class
})
public class CommonWebAutoConfiguration {}
