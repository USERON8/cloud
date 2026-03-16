package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public class BizException extends BaseException {

    public BizException(ResultCode resultCode) {
        this(resultCode, resultCode == null ? null : resultCode.getMessage(), null);
    }

    public BizException(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public BizException(ResultCode resultCode, String message, Throwable cause) {
        super(
                resultCode,
                resolveMessage(resultCode, message),
                cause,
                ResultCodeHttpStatusMapper.resolve(resultCode),
                ErrorCategory.BIZ,
                false);
    }

    public BizException(String message) {
        this(ResultCode.BUSINESS_ERROR, message, null);
    }

    public BizException(String message, Throwable cause) {
        this(ResultCode.BUSINESS_ERROR, message, cause);
    }

    public BizException(int code, String message) {
        this(code, message, null);
    }

    public BizException(int code, String message, Throwable cause) {
        super(code, message, cause, 400, ErrorCategory.BIZ, false);
    }

    private static String resolveMessage(ResultCode resultCode, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return resultCode == null ? ResultCode.BUSINESS_ERROR.getMessage() : resultCode.getMessage();
    }

}
