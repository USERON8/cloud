package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExceptionReporter {

    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    public ExceptionReporter(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void reportBiz(BizException e, HttpServletRequest request) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        log.warn("[BIZ] method={} uri={} code={} msg={}", method, uri, e.getCode(), e.getMessage());
        increment(
                "exception.biz",
                "code",
                String.valueOf(e.getCode()),
                "uri",
                sanitizeUri(uri));
    }

    public void reportSystem(SystemException e, HttpServletRequest request) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        log.error("[SYS] method={} uri={} code={} msg={}", method, uri, e.getCode(), e.getMessage(), e);
        increment(
                "exception.system",
                "code",
                String.valueOf(e.getCode()),
                "service",
                serviceName);
    }

    public void reportRemote(RemoteException e, HttpServletRequest request) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        log.error("[REMOTE] method={} uri={} code={} msg={}", method, uri, e.getCode(), e.getMessage(), e);
        increment(
                "exception.remote",
                "code",
                String.valueOf(e.getCode()),
                "service",
                serviceName);
    }

    public void reportSystem(ResultCode resultCode, Throwable e, HttpServletRequest request, String message) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        int code = resultCode == null ? ResultCode.SYSTEM_ERROR.getCode() : resultCode.getCode();
        log.error("[SYS] method={} uri={} code={} msg={}", method, uri, code, message, e);
        increment(
                "exception.system",
                "code",
                String.valueOf(code),
                "service",
                serviceName);
    }

    public void reportWarn(String category, HttpServletRequest request, String message) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        log.warn("[{}] method={} uri={} msg={}", category, method, uri, message);
    }

    public void reportError(String category, HttpServletRequest request, Throwable e) {
        String method = safeMethod(request);
        String uri = safeUri(request);
        log.error("[{}] method={} uri={}", category, method, uri, e);
    }

    private void increment(String name, String... tags) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(name, tags).increment();
    }

    private String safeMethod(HttpServletRequest request) {
        if (request == null || request.getMethod() == null) {
            return "unknown";
        }
        return request.getMethod();
    }

    private String safeUri(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        return uri;
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        return uri.replaceAll("/\\d+", "/{id}");
    }
}
