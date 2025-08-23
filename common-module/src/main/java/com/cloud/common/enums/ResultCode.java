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
    CONFLICT(409, "请求冲突"),

    // 业务错误 5xx
    ERROR(500, "系统内部错误"),
    PARAM_ERROR(501, "参数校验失败"),
    BUSINESS_ERROR(502, "业务处理失败"),

    // 系统级别错误 1xxx
    SYSTEM_ERROR(1001, "系统错误"),
    SYSTEM_BUSY(1002, "系统繁忙，请稍后再试"),
    SYSTEM_TIMEOUT(1003, "系统超时"),
    SYSTEM_NOT_IMPLEMENTED(1004, "功能未实现"),

    // 权限相关错误 2xxx
    PERMISSION_DENIED(2001, "权限不足"),
    ACCESS_DENIED(2002, "访问被拒绝"),
    ROLE_NOT_FOUND(2003, "角色不存在"),

    // 参数校验相关错误 3xxx
    VALIDATION_ERROR(3001, "参数校验失败"),
    MISSING_PARAMETER(3002, "缺少必要参数"),
    INVALID_PARAMETER(3003, "参数格式不正确"),

    // 资源相关错误 4xxx
    RESOURCE_NOT_FOUND(4001, "资源不存在"),
    RESOURCE_ALREADY_EXISTS(4002, "资源已存在"),
    RESOURCE_LOCKED(4003, "资源被锁定"),

    // 并发相关错误 5xxx
    CONCURRENT_MODIFICATION(5001, "并发修改冲突"),
    OPTIMISTIC_LOCK_ERROR(5002, "乐观锁异常"),

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

    // 用户服务相关错误 8xxx
    USER_ALREADY_EXISTS(8001, "用户已存在"),
    USER_CREATE_FAILED(8002, "用户创建失败"),
    USER_UPDATE_FAILED(8003, "用户更新失败"),
    USER_DELETE_FAILED(8004, "用户删除失败"),
    USER_QUERY_FAILED(8005, "用户查询失败"),
    USER_NOT_MERCHANT(8006, "用户不是商家类型"),
    PARAM_VALIDATION_FAILED(8007, "用户ID不能为空"),

    // 文件上传相关错误 9xxx
    FILE_IS_EMPTY(9001, "上传的文件为空"),
    FILE_SIZE_EXCEEDED(9002, "文件大小超出限制"),
    UPLOAD_FAILED(9003, "文件上传失败");

    private final Integer code;
    private final String message;
}