package com.cloud.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数校验异常类
 * 用于处理参数校验失败的情况
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidationException extends BusinessException {
    private String field;
    private Object rejectedValue;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(int code, String message) {
        super(code, message);
    }

    public ValidationException(String field, Object rejectedValue, String message) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public ValidationException(int code, String field, Object rejectedValue, String message) {
        super(code, message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }
}