package com.cloud.common.config;

import com.cloud.common.domain.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应结果增强器
 * 自动包装返回结果为统一格式
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果返回类型已经是Result，则不需要包装
        return !returnType.getParameterType().equals(Result.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 如果body已经是Result类型，直接返回
        if (body instanceof Result) {
            return body;
        }

        // 包装成功结果
        Result<Object> result = Result.success(body);

        // 如果返回类型是String，需要特殊处理
        if (returnType.getParameterType().equals(String.class)) {
            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                log.error("JSON序列化失败", e);
                return Result.error("响应数据序列化失败");
            }
        }

        return result;
    }
}