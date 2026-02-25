package com.cloud.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

@Component
public class ApiMessageResolver {

    private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    public String message(String english, String chinese) {
        if (isChineseRequest()) {
            return chinese;
        }
        return english;
    }

    public String messageWithDetail(String englishPrefix, String chinesePrefix, String detail) {
        String normalizedDetail = detail == null ? "" : detail;
        if (isChineseRequest()) {
            return chinesePrefix + normalizedDetail;
        }
        return englishPrefix + normalizedDetail;
    }

    private boolean isChineseRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return false;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String acceptLanguage = request.getHeader(HEADER_ACCEPT_LANGUAGE);
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return false;
        }
        String normalized = acceptLanguage.toLowerCase(Locale.ROOT);
        return normalized.startsWith("zh") || normalized.contains(",zh") || normalized.contains("zh-");
    }
}
