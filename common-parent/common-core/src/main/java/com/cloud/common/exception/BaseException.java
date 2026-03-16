package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.trace.TraceIdUtil;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final int code;
    private final int httpStatus;
    private final ErrorCategory category;
    private final boolean alert;
    private final String traceId;

    protected BaseException(
            ResultCode resultCode,
            String message,
            Throwable cause,
            int httpStatus,
            ErrorCategory category,
            boolean alert) {
        super(message, cause);
        this.code = resultCode == null ? ResultCode.ERROR.getCode() : resultCode.getCode();
        this.httpStatus = httpStatus;
        this.category = category;
        this.alert = alert;
        this.traceId = TraceIdUtil.currentTraceId();
    }

    protected BaseException(
            int code,
            String message,
            Throwable cause,
            int httpStatus,
            ErrorCategory category,
            boolean alert) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.category = category;
        this.alert = alert;
        this.traceId = TraceIdUtil.currentTraceId();
    }
}
