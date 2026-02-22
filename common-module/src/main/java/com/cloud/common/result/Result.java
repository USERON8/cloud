package com.cloud.common.result;

import com.cloud.common.enums.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;






@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    


    private Integer code;

    


    private String message;

    


    private T data;

    


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

    


    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    


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

    


    public static <T> Result<T> forbidden() {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }

    


    public static <T> Result<T> notFound() {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage(), null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), message, null);
    }

    


    public static <T> Result<T> badRequest() {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    


    public static <T> Result<T> systemError() {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), null);
    }

    public static <T> Result<T> systemError(String message) {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    


    public static <T> Result<T> businessError() {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), ResultCode.BUSINESS_ERROR.getMessage(), null);
    }

    public static <T> Result<T> businessError(String message) {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    


    public static <T> Result<T> paramError() {
        return new Result<>(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMessage(), null);
    }

    public static <T> Result<T> paramError(String message) {
        return new Result<>(ResultCode.PARAM_ERROR.getCode(), message, null);
    }

    


    public static <T> Result<T> unauthorized() {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    


    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
