package com.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    // 业务错误 5xx
    ERROR(500, "系统内部错误"),
    PARAM_ERROR(501, "参数校验失败"),
    BUSINESS_ERROR(502, "业务处理失败"),

    // 库存相关错误 6xxx
    STOCK_NOT_FOUND(6001, "商品库存不存在"),
    STOCK_INSUFFICIENT(6002, "库存不足"),
    STOCK_DEDUCT_FAILED(6003, "库存扣减失败"),
    STOCK_ADD_FAILED(6004, "库存增加失败"),

    // 数据库相关错误 7xxx
    DB_ERROR(7001, "数据库操作失败"),
    DB_DUPLICATE_KEY(7002, "数据重复"),
    DB_CONSTRAINT_VIOLATION(7003, "数据约束违反"),
    USER_NOT_FOUND(7004, "用户不存在"),
    USERNAME_OR_PASSWORD_ERROR(7005, "用户名或密码错误"),
    SYSTEM_ERROR(7006, "系统错误");

    private final Integer code;
    private final String message;
}