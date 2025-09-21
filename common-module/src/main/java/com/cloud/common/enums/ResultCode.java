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

    // 库存相关错误 6xxx (库存服务专用错误码范围: 6000-6999)
    STOCK_NOT_FOUND(6001, "商品库存不存在"),
    STOCK_INSUFFICIENT(6002, "库存不足"),
    STOCK_DEDUCT_FAILED(6003, "库存扣减失败"),
    STOCK_ADD_FAILED(6004, "库存增加失败"),
    STOCK_QUERY_FAILED(6005, "库存查询失败"),

    // 数据库相关错误 7xxx
    DB_ERROR(7001, "数据库操作失败"),
    DB_DUPLICATE_KEY(7002, "数据重复"),
    DB_CONSTRAINT_VIOLATION(7003, "数据约束违反"),
    USER_NOT_FOUND(7004, "用户不存在"),
    USERNAME_OR_PASSWORD_ERROR(7005, "用户名或密码错误"),

    // 用户服务相关错误 8xxx (用户服务专用错误码范围: 8000-8999)
    USER_ALREADY_EXISTS(8001, "用户已存在"),
    USER_CREATE_FAILED(8002, "用户创建失败"),
    USER_UPDATE_FAILED(8003, "用户更新失败"),
    USER_DELETE_FAILED(8004, "用户删除失败"),
    USER_QUERY_FAILED(8005, "用户查询失败"),
    USER_NOT_MERCHANT(8006, "用户不是商家类型"),
    PARAM_VALIDATION_FAILED(8007, "用户ID不能为空"),
    USER_TYPE_MISMATCH(8008, "用户类型不匹配"),
    PASSWORD_ERROR(8009, "密码错误"),
    USER_DISABLED(8010, "用户账户已被禁用"),

    // 商品服务相关错误 9xxx (商品服务专用错误码范围: 9000-9999)
    PRODUCT_NOT_FOUND(9001, "商品不存在"),
    PRODUCT_CREATE_FAILED(9002, "商品创建失败"),
    PRODUCT_UPDATE_FAILED(9003, "商品更新失败"),
    PRODUCT_DELETE_FAILED(9004, "商品删除失败"),
    PRODUCT_CATEGORY_NOT_FOUND(9005, "商品分类不存在"),
    PRODUCT_STATUS_ERROR(9006, "商品状态异常"),
    PRODUCT_QUERY_FAILED(9007, "商品查询失败"),
    PRODUCT_ALREADY_EXISTS(9008, "商品已存在"),
    CATEGORY_NOT_FOUND(9009, "分类不存在"),

    // 订单服务相关错误 10xxx (订单服务专用错误码范围: 10000-10999)
    ORDER_NOT_FOUND(10001, "订单不存在"),
    ORDER_CREATE_FAILED(10002, "订单创建失败"),
    ORDER_UPDATE_FAILED(10003, "订单更新失败"),
    ORDER_DELETE_FAILED(10004, "订单删除失败"),
    ORDER_STATUS_ERROR(10005, "订单状态异常"),
    ORDER_QUERY_FAILED(10006, "订单查询失败"),

    // 支付服务相关错误 11xxx (支付服务专用错误码范围: 11000-11999)
    PAYMENT_NOT_FOUND(11001, "支付记录不存在"),
    PAYMENT_CREATE_FAILED(11002, "支付记录创建失败"),
    PAYMENT_UPDATE_FAILED(11003, "支付记录更新失败"),
    PAYMENT_DELETE_FAILED(11004, "支付记录删除失败"),
    PAYMENT_STATUS_ERROR(11005, "支付状态异常"),
    PAYMENT_REFUND_FAILED(11006, "退款失败"),
    PAYMENT_QUERY_FAILED(11007, "支付查询失败"),

    // 商家服务相关错误 12xxx (商家服务专用错误码范围: 12000-12999)
    MERCHANT_NOT_FOUND(12001, "商家不存在"),
    MERCHANT_CREATE_FAILED(12002, "商家创建失败"),
    MERCHANT_UPDATE_FAILED(12003, "商家更新失败"),
    MERCHANT_DELETE_FAILED(12004, "商家删除失败"),
    MERCHANT_STATUS_ERROR(12005, "商家状态异常"),
    MERCHANT_QUERY_FAILED(12006, "商家查询失败"),

    // 管理服务相关错误 13xxx (管理服务专用错误码范围: 13000-13999)
    ADMIN_NOT_FOUND(13001, "管理员不存在"),
    ADMIN_CREATE_FAILED(13002, "管理员创建失败"),
    ADMIN_UPDATE_FAILED(13003, "管理员更新失败"),
    ADMIN_DELETE_FAILED(13004, "管理员删除失败"),
    ADMIN_STATUS_ERROR(13005, "管理员状态异常"),
    ADMIN_QUERY_FAILED(13006, "管理员查询失败"),

    // 搜索服务相关错误 14xxx (搜索服务专用错误码范围: 14000-14999)
    SEARCH_FAILED(14001, "搜索失败"),
    SEARCH_INDEX_ERROR(14002, "索引异常"),

    // 文件上传相关错误 15xxx
    FILE_IS_EMPTY(15001, "上传的文件为空"),
    FILE_SIZE_EXCEEDED(15002, "文件大小超出限制"),
    UPLOAD_FAILED(15003, "文件上传失败"),

    // 日志服务相关错误 16xxx (日志服务专用错误码范围: 16000-16999)
    LOG_CREATE_FAILED(16001, "日志创建失败"),
    LOG_UPDATE_FAILED(16002, "日志更新失败"),
    LOG_DELETE_FAILED(16003, "日志删除失败"),
    LOG_QUERY_FAILED(16004, "日志查询失败"),

    // 认证服务相关错误 17xxx (认证服务专用错误码范围: 17000-17999)
    // OAuth2.1标准相关错误
    OAUTH2_INVALID_REQUEST(17001, "OAuth2请求参数无效"),
    OAUTH2_INVALID_CLIENT(17002, "OAuth2客户端无效"),
    OAUTH2_INVALID_GRANT(17003, "OAuth2授权无效"),
    OAUTH2_UNAUTHORIZED_CLIENT(17004, "OAuth2客户端未授权"),
    OAUTH2_UNSUPPORTED_GRANT_TYPE(17005, "OAuth2不支持的授权类型"),
    OAUTH2_INVALID_SCOPE(17006, "OAuth2权限范围无效"),
    OAUTH2_ACCESS_DENIED(17007, "OAuth2访问被拒绝"),
    OAUTH2_SERVER_ERROR(17008, "OAuth2服务器内部错误"),

    // JWT令牌相关错误
    JWT_TOKEN_INVALID(17011, "JWT令牌无效"),
    JWT_TOKEN_EXPIRED(17012, "JWT令牌已过期"),
    JWT_TOKEN_MALFORMED(17013, "JWT令牌格式错误"),
    JWT_SIGNATURE_INVALID(17014, "JWT令牌签名验证失败"),
    JWT_TOKEN_NOT_FOUND(17015, "未找到JWT令牌"),
    JWT_GENERATION_FAILED(17016, "JWT令牌生成失败"),

    // 认证相关错误
    AUTHENTICATION_FAILED(17021, "身份认证失败"),
    BAD_CREDENTIALS(17022, "用户名或密码错误"),
    ACCOUNT_LOCKED(17023, "账户已被锁定"),
    ACCOUNT_EXPIRED(17024, "账户已过期"),
    CREDENTIALS_EXPIRED(17025, "凭证已过期"),
    TOKEN_GENERATION_FAILED(17026, "令牌生成失败"),
    TOKEN_REVOCATION_FAILED(17027, "令牌撤销失败"),

    // PKCE相关错误 (OAuth2.1标准)
    PKCE_CHALLENGE_MISSING(17031, "PKCE质问参数缺失"),
    PKCE_VERIFIER_INVALID(17032, "PKCE验证器无效"),
    PKCE_METHOD_UNSUPPORTED(17033, "PKCE方法不支持"),

    // 客户端相关错误
    CLIENT_REGISTRATION_FAILED(17041, "客户端注册失败"),
    CLIENT_NOT_FOUND(17042, "客户端不存在"),
    CLIENT_AUTHENTICATION_FAILED(17043, "客户端认证失败"),

    // 授权相关错误
    AUTHORIZATION_CODE_INVALID(17051, "授权码无效"),
    AUTHORIZATION_CODE_EXPIRED(17052, "授权码已过期"),
    AUTHORIZATION_CODE_USED(17053, "授权码已使用"),
    REFRESH_TOKEN_INVALID(17054, "刷新令牌无效"),
    REFRESH_TOKEN_EXPIRED(17055, "刷新令牌已过期");

    private final Integer code;
    private final String message;
}