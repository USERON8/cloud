package com.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "Success"),

    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    CONFLICT(409, "Conflict"),

    ERROR(500, "Internal error"),
    PARAM_ERROR(501, "Parameter error"),
    BUSINESS_ERROR(502, "Business error"),

    SYSTEM_ERROR(1001, "System error"),
    SYSTEM_BUSY(1002, "System busy"),
    SYSTEM_TIMEOUT(1003, "System timeout"),
    SYSTEM_NOT_IMPLEMENTED(1004, "Not implemented"),

    PERMISSION_DENIED(2001, "Permission denied"),
    ACCESS_DENIED(2002, "Access denied"),
    ROLE_NOT_FOUND(2003, "Role not found"),

    VALIDATION_ERROR(3001, "Validation error"),
    MISSING_PARAMETER(3002, "Missing parameter"),
    INVALID_PARAMETER(3003, "Invalid parameter"),

    RESOURCE_NOT_FOUND(4001, "Resource not found"),
    RESOURCE_ALREADY_EXISTS(4002, "Resource already exists"),
    RESOURCE_LOCKED(4003, "Resource locked"),

    CONCURRENT_MODIFICATION(5001, "Concurrent modification"),
    OPTIMISTIC_LOCK_ERROR(5002, "Optimistic lock error"),

    STOCK_NOT_FOUND(6001, "Stock not found"),
    STOCK_INSUFFICIENT(6002, "Insufficient stock"),
    STOCK_DEDUCT_FAILED(6003, "Stock deduction failed"),
    STOCK_ADD_FAILED(6004, "Stock increment failed"),
    STOCK_QUERY_FAILED(6005, "Stock query failed"),

    DB_ERROR(7001, "Database error"),
    DB_DUPLICATE_KEY(7002, "Duplicate data"),
    DB_CONSTRAINT_VIOLATION(7003, "Constraint violation"),
    USER_NOT_FOUND(7004, "User not found"),
    USERNAME_OR_PASSWORD_ERROR(7005, "Username or password error"),

    USER_ALREADY_EXISTS(8001, "User already exists"),
    USER_CREATE_FAILED(8002, "User creation failed"),
    USER_UPDATE_FAILED(8003, "User update failed"),
    USER_DELETE_FAILED(8004, "User deletion failed"),
    USER_QUERY_FAILED(8005, "User query failed"),
    USER_NOT_MERCHANT(8006, "User is not a merchant"),
    PARAM_VALIDATION_FAILED(8007, "Parameter validation failed"),
    USER_TYPE_MISMATCH(8008, "User type mismatch"),
    PASSWORD_ERROR(8009, "Password error"),
    USER_DISABLED(8010, "User is disabled"),

    PRODUCT_NOT_FOUND(9001, "Product not found"),
    PRODUCT_CREATE_FAILED(9002, "Product creation failed"),
    PRODUCT_UPDATE_FAILED(9003, "Product update failed"),
    PRODUCT_DELETE_FAILED(9004, "Product deletion failed"),
    PRODUCT_CATEGORY_NOT_FOUND(9005, "Product category not found"),
    PRODUCT_STATUS_ERROR(9006, "Product status error"),
    PRODUCT_QUERY_FAILED(9007, "Product query failed"),
    PRODUCT_ALREADY_EXISTS(9008, "Product already exists"),
    CATEGORY_NOT_FOUND(9009, "Category not found"),

    ORDER_NOT_FOUND(10001, "Order not found"),
    ORDER_CREATE_FAILED(10002, "Order creation failed"),
    ORDER_UPDATE_FAILED(10003, "Order update failed"),
    ORDER_DELETE_FAILED(10004, "Order deletion failed"),
    ORDER_STATUS_ERROR(10005, "Order status error"),
    ORDER_QUERY_FAILED(10006, "Order query failed"),

    PAYMENT_NOT_FOUND(11001, "Payment record not found"),
    PAYMENT_CREATE_FAILED(11002, "Payment record creation failed"),
    PAYMENT_UPDATE_FAILED(11003, "Payment record update failed"),
    PAYMENT_DELETE_FAILED(11004, "Payment record deletion failed"),
    PAYMENT_STATUS_ERROR(11005, "Payment status error"),
    PAYMENT_REFUND_FAILED(11006, "Refund failed"),
    PAYMENT_QUERY_FAILED(11007, "Payment query failed"),

    MERCHANT_NOT_FOUND(12001, "Merchant not found"),
    MERCHANT_CREATE_FAILED(12002, "Merchant creation failed"),
    MERCHANT_UPDATE_FAILED(12003, "Merchant update failed"),
    MERCHANT_DELETE_FAILED(12004, "Merchant deletion failed"),
    MERCHANT_STATUS_ERROR(12005, "Merchant status error"),
    MERCHANT_QUERY_FAILED(12006, "Merchant query failed"),

    ADMIN_NOT_FOUND(13001, "Admin not found"),
    ADMIN_CREATE_FAILED(13002, "Admin creation failed"),
    ADMIN_UPDATE_FAILED(13003, "Admin update failed"),
    ADMIN_DELETE_FAILED(13004, "Admin deletion failed"),
    ADMIN_STATUS_ERROR(13005, "Admin status error"),
    ADMIN_QUERY_FAILED(13006, "Admin query failed"),

    SEARCH_FAILED(14001, "Search failed"),
    SEARCH_INDEX_ERROR(14002, "Search index error"),

    FILE_IS_EMPTY(15001, "File is empty"),
    FILE_SIZE_EXCEEDED(15002, "File size exceeded"),
    UPLOAD_FAILED(15003, "Upload failed"),

    LOG_CREATE_FAILED(16001, "Log creation failed"),
    LOG_UPDATE_FAILED(16002, "Log update failed"),
    LOG_DELETE_FAILED(16003, "Log deletion failed"),
    LOG_QUERY_FAILED(16004, "Log query failed"),

    OAUTH2_INVALID_REQUEST(17001, "OAuth2 invalid request"),
    OAUTH2_INVALID_CLIENT(17002, "OAuth2 invalid client"),
    OAUTH2_INVALID_GRANT(17003, "OAuth2 invalid grant"),
    OAUTH2_UNAUTHORIZED_CLIENT(17004, "OAuth2 unauthorized client"),
    OAUTH2_UNSUPPORTED_GRANT_TYPE(17005, "OAuth2 unsupported grant type"),
    OAUTH2_INVALID_SCOPE(17006, "OAuth2 invalid scope"),
    OAUTH2_ACCESS_DENIED(17007, "OAuth2 access denied"),
    OAUTH2_SERVER_ERROR(17008, "OAuth2 server error"),

    JWT_TOKEN_INVALID(17011, "JWT token invalid"),
    JWT_TOKEN_EXPIRED(17012, "JWT token expired"),
    JWT_TOKEN_MALFORMED(17013, "JWT token malformed"),
    JWT_SIGNATURE_INVALID(17014, "JWT signature invalid"),
    JWT_TOKEN_NOT_FOUND(17015, "JWT token not found"),
    JWT_GENERATION_FAILED(17016, "JWT generation failed"),

    AUTHENTICATION_FAILED(17021, "Authentication failed"),
    BAD_CREDENTIALS(17022, "Bad credentials"),
    ACCOUNT_LOCKED(17023, "Account locked"),
    ACCOUNT_EXPIRED(17024, "Account expired"),
    CREDENTIALS_EXPIRED(17025, "Credentials expired"),
    TOKEN_GENERATION_FAILED(17026, "Token generation failed"),
    TOKEN_REVOCATION_FAILED(17027, "Token revocation failed"),

    PKCE_CHALLENGE_MISSING(17031, "PKCE challenge missing"),
    PKCE_VERIFIER_INVALID(17032, "PKCE verifier invalid"),
    PKCE_METHOD_UNSUPPORTED(17033, "PKCE method unsupported"),

    CLIENT_REGISTRATION_FAILED(17041, "Client registration failed"),
    CLIENT_NOT_FOUND(17042, "Client not found"),
    CLIENT_AUTHENTICATION_FAILED(17043, "Client authentication failed"),

    AUTHORIZATION_CODE_INVALID(17051, "Authorization code invalid"),
    AUTHORIZATION_CODE_EXPIRED(17052, "Authorization code expired"),
    AUTHORIZATION_CODE_USED(17053, "Authorization code already used"),
    REFRESH_TOKEN_INVALID(17054, "Refresh token invalid"),
    REFRESH_TOKEN_EXPIRED(17055, "Refresh token expired");

    private final Integer code;
    private final String message;
}
