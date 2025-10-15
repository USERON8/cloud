package com.cloud.common.result;

import com.cloud.common.enums.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果类
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * 失败响应（fail作为error的别名）
     */
    public static <T> Result<T> fail() {
        return error();
    }

    public static <T> Result<T> fail(String message) {
        return error(message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return error(code, message);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return error(resultCode);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return error(resultCode, message);
    }

    /**
     * 权限不足响应
     */
    public static <T> Result<T> forbidden() {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }

    /**
     * 资源不存在响应
     */
    public static <T> Result<T> notFound() {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage(), null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), message, null);
    }

    /**
     * 请求参数错误响应
     */
    public static <T> Result<T> badRequest() {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    /**
     * 系统错误响应
     */
    public static <T> Result<T> systemError() {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), null);
    }

    public static <T> Result<T> systemError(String message) {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    /**
     * 业务错误响应
     */
    public static <T> Result<T> businessError() {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), ResultCode.BUSINESS_ERROR.getMessage(), null);
    }

    public static <T> Result<T> businessError(String message) {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    /**
     * 参数校验失败响应
     */
    public static <T> Result<T> paramError() {
        return new Result<>(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMessage(), null);
    }

    public static <T> Result<T> paramError(String message) {
        return new Result<>(ResultCode.PARAM_ERROR.getCode(), message, null);
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}