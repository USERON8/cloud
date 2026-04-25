package com.cloud.payment.controller;

import com.cloud.common.annotation.RawResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RawResponse
@RestControllerAdvice(assignableTypes = AlipayCallbackController.class)
public class AlipayCallbackExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex) {
    log.warn("Handle Alipay notify callback failed", ex);
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("failure");
  }
}
