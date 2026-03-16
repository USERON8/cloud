package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public class RemoteException extends BaseException {

    public RemoteException(ResultCode resultCode) {
        this(resultCode, resultCode == null ? null : resultCode.getMessage(), null);
    }

    public RemoteException(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public RemoteException(ResultCode resultCode, String message, Throwable cause) {
        super(
                resultCode,
                resolveMessage(resultCode, message),
                cause,
                503,
                ErrorCategory.REMOTE,
                true);
    }

    public RemoteException(String message, Throwable cause) {
        this(ResultCode.REMOTE_SERVICE_UNAVAILABLE, message, cause);
    }

    private static String resolveMessage(ResultCode resultCode, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return resultCode == null ? ResultCode.REMOTE_SERVICE_UNAVAILABLE.getMessage() : resultCode.getMessage();
    }
}
