package com.cloud.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统异常类
 * 用于处理系统级别的异常，如数据库访问异常、网络异常等
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemException extends RuntimeException {
    private int code;
    private String message;

    public SystemException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public SystemException(String message) {
        super(message);
        this.message = message;
    }

    public SystemException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}