package com.cloud.common.config;

import com.cloud.common.result.Result;
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

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !Result.class.equals(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }

        // Internal Feign endpoints must return raw DTOs to keep contract compatibility.
        String path = request.getURI().getPath();
        if (path != null && path.startsWith("/internal/")) {
            return body;
        }

        Result<Object> result = Result.success(body);

        if (String.class.equals(returnType.getParameterType())) {
            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize Result response", e);
                return Result.error("Failed to serialize response body");
            }
        }

        return result;
    }
}
