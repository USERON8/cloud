package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常基类
 * 用于表示业务逻辑中的异常情况，包含错误码和错误消息
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private int code;
    private String message;

    /**
     * 使用指定的错误码和消息创建业务异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用默认错误码和指定消息创建业务异常
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.message = message;
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    /**
     * 使用结果码创建业务异常
     *
     * @param resultCode 结果码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 使用结果码和自定义消息创建业务异常
     *
     * @param resultCode 结果码枚举
     * @param message 自定义错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}