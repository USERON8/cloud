package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public class SystemException extends BaseException {

    public SystemException(ResultCode resultCode) {
        this(resultCode, resultCode == null ? null : resultCode.getMessage(), null);
    }

    public SystemException(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public SystemException(ResultCode resultCode, String message, Throwable cause) {
        super(
                resultCode,
                resolveMessage(resultCode, message),
                cause,
                500,
                ErrorCategory.SYSTEM,
                true);
    }

    public SystemException(String message) {
        this(ResultCode.SYSTEM_ERROR, message, null);
    }

    public SystemException(String message, Throwable cause) {
        this(ResultCode.SYSTEM_ERROR, message, cause);
    }

    public SystemException(int code, String message) {
        this(code, message, null);
    }

    public SystemException(int code, String message, Throwable cause) {
        super(code, message, cause, 500, ErrorCategory.SYSTEM, true);
    }

    private static String resolveMessage(ResultCode resultCode, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return resultCode == null ? ResultCode.SYSTEM_ERROR.getMessage() : resultCode.getMessage();
    }
}
