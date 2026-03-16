package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public final class ResultCodeHttpStatusMapper {

    private ResultCodeHttpStatusMapper() {}

    public static int resolve(ResultCode resultCode) {
        if (resultCode == null) {
            return 400;
        }
        return switch (resultCode) {
            case BAD_REQUEST,
                    PARAM_ERROR,
                    VALIDATION_ERROR,
                    MISSING_PARAMETER,
                    INVALID_PARAMETER,
                    PARAM_VALIDATION_FAILED,
                    JWT_TOKEN_INVALID,
                    JWT_TOKEN_EXPIRED,
                    JWT_TOKEN_MALFORMED,
                    JWT_SIGNATURE_INVALID,
                    JWT_TOKEN_NOT_FOUND,
                    OAUTH2_INVALID_REQUEST,
                    OAUTH2_INVALID_GRANT,
                    OAUTH2_UNSUPPORTED_GRANT_TYPE,
                    OAUTH2_INVALID_SCOPE,
                    PKCE_CHALLENGE_MISSING,
                    PKCE_VERIFIER_INVALID,
                    PKCE_METHOD_UNSUPPORTED -> 400;
            case UNAUTHORIZED,
                    AUTHENTICATION_FAILED,
                    BAD_CREDENTIALS,
                    TOKEN_GENERATION_FAILED,
                    TOKEN_REVOCATION_FAILED -> 401;
            case FORBIDDEN,
                    ACCESS_DENIED,
                    PERMISSION_DENIED,
                    ROLE_MISMATCH,
                    USER_DISABLED,
                    ACCOUNT_LOCKED,
                    ACCOUNT_EXPIRED,
                    CREDENTIALS_EXPIRED -> 403;
            case NOT_FOUND,
                    RESOURCE_NOT_FOUND,
                    USER_NOT_FOUND,
                    PRODUCT_NOT_FOUND,
                    CATEGORY_NOT_FOUND,
                    ORDER_NOT_FOUND,
                    PAYMENT_NOT_FOUND,
                    MERCHANT_NOT_FOUND,
                    ADMIN_NOT_FOUND,
                    ROLE_NOT_FOUND,
                    CLIENT_NOT_FOUND -> 404;
            case METHOD_NOT_ALLOWED -> 405;
            case CONFLICT,
                    ORDER_STATUS_ERROR,
                    PRODUCT_STATUS_ERROR,
                    STOCK_INSUFFICIENT,
                    MERCHANT_STATUS_ERROR,
                    ADMIN_STATUS_ERROR,
                    PAYMENT_STATUS_ERROR,
                    DB_DUPLICATE_KEY -> 409;
            case RATE_LIMITED -> 429;
            case REMOTE_SERVICE_UNAVAILABLE -> 503;
            case REMOTE_SERVICE_TIMEOUT -> 504;
            case SYSTEM_NOT_IMPLEMENTED -> 501;
            case SYSTEM_ERROR,
                    SYSTEM_BUSY,
                    SYSTEM_TIMEOUT,
                    DB_ERROR,
                    DB_CONSTRAINT_VIOLATION,
                    SEARCH_INDEX_ERROR -> 500;
            default -> 400;
        };
    }
}
