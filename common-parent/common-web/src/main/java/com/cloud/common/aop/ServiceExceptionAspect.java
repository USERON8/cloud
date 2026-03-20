package com.cloud.common.aop;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceExceptionAspect {

  @Pointcut("within(@org.springframework.stereotype.Service *)")
  public void serviceLayer() {}

  @Pointcut(
      "within(com.cloud..infrastructure..*)"
          + " || within(com.cloud..cache..*)"
          + " || within(com.cloud.user.cache.warmup..*)"
          + " || within(com.cloud..tcc..*)"
          + " || within(com.cloud..task..*)"
          + " || within(com.cloud..outbox..*)"
          + " || within(com.cloud.search.messaging..*)"
          + " || within(com.cloud.search.task..*)"
          + " || within(com.cloud.common.messaging..*)"
          + " || within(com.cloud.payment.service.support..*)"
          + " || within(com.cloud.gateway.config..*)"
          + " || within(com.cloud.gateway.controller..*)"
          + " || within(com.cloud.gateway.cache..*)")
  public void excludedPackages() {}

  @Around("serviceLayer() && !excludedPackages()")
  public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
    try {
      return pjp.proceed();
    } catch (BizException | SystemException | RemoteException e) {
      throw e;
    } catch (DataAccessException e) {
      log.error(
          "[SERVICE-DB] {}.{}",
          pjp.getSignature().getDeclaringTypeName(),
          pjp.getSignature().getName(),
          e);
      throw new SystemException(ResultCode.DB_ERROR, "Database operation failed", e);
    } catch (Exception e) {
      log.error(
          "[SERVICE-UNKNOWN] {}.{}",
          pjp.getSignature().getDeclaringTypeName(),
          pjp.getSignature().getName(),
          e);
      throw new SystemException(ResultCode.SYSTEM_ERROR, "Internal service error", e);
    }
  }
}
