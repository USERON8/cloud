package com.cloud.common.web;

import com.cloud.common.result.Result;
import com.cloud.common.trace.TraceIdUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.cloud")
public class TraceResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        String traceId = TraceIdUtil.currentTraceId();
        if (traceId != null
                && !traceId.isBlank()
                && !response.getHeaders().containsKey("X-Trace-Id")) {
            response.getHeaders().set("X-Trace-Id", traceId);
        }
        if (body instanceof Result<?> result && !result.isSuccess()) {
            if (traceId != null && !traceId.isBlank()) {
                result.withTraceId(traceId);
            }
        }
        return body;
    }
}
