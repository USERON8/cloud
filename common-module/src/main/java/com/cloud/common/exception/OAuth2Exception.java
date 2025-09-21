package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OAuth2.1相关业务异常类
 * 用于处理OAuth2.1授权服务器和资源服务器的业务异常
 * <p>
 * 支持OAuth2.1标准错误类型：
 * - invalid_request
 * - invalid_client
 * - invalid_grant
 * - unauthorized_client
 * - unsupported_grant_type
 * - invalid_scope
 * - access_denied
 * - server_error
 *
 * @author what's up
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OAuth2Exception extends BusinessException {

    private String error;        // OAuth2标准错误类型
    private String errorDescription; // 错误描述
    private String errorUri;     // 错误详情URI（可选）
    private String state;        // 状态参数（可选）

    public OAuth2Exception(String error, String errorDescription) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public OAuth2Exception(ResultCode resultCode) {
        super(resultCode);
        this.error = mapResultCodeToOAuth2Error(resultCode);
        this.errorDescription = resultCode.getMessage();
    }

    public OAuth2Exception(ResultCode resultCode, String errorDescription) {
        super(resultCode, errorDescription);
        this.error = mapResultCodeToOAuth2Error(resultCode);
        this.errorDescription = errorDescription;
    }

    public OAuth2Exception(String error, String errorDescription, String state) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
        this.state = state;
    }

    public OAuth2Exception(String error, String errorDescription, String errorUri, String state) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorUri = errorUri;
        this.state = state;
    }

    /**
     * 创建invalid_request异常
     */
    public static OAuth2Exception invalidRequest(String description) {
        return new OAuth2Exception("invalid_request", description);
    }

    /**
     * 创建invalid_client异常
     */
    public static OAuth2Exception invalidClient(String description) {
        return new OAuth2Exception("invalid_client", description);
    }

    /**
     * 创建invalid_grant异常
     */
    public static OAuth2Exception invalidGrant(String description) {
        return new OAuth2Exception("invalid_grant", description);
    }

    /**
     * 创建unauthorized_client异常
     */
    public static OAuth2Exception unauthorizedClient(String description) {
        return new OAuth2Exception("unauthorized_client", description);
    }

    /**
     * 创建unsupported_grant_type异常
     */
    public static OAuth2Exception unsupportedGrantType(String description) {
        return new OAuth2Exception("unsupported_grant_type", description);
    }

    /**
     * 创建invalid_scope异常
     */
    public static OAuth2Exception invalidScope(String description) {
        return new OAuth2Exception("invalid_scope", description);
    }

    /**
     * 创建access_denied异常
     */
    public static OAuth2Exception accessDenied(String description) {
        return new OAuth2Exception("access_denied", description);
    }

    /**
     * 创建server_error异常
     */
    public static OAuth2Exception serverError(String description) {
        return new OAuth2Exception("server_error", description);
    }

    /**
     * 将ResultCode映射为OAuth2标准错误类型
     */
    private String mapResultCodeToOAuth2Error(ResultCode resultCode) {
        return switch (resultCode) {
            case OAUTH2_INVALID_REQUEST -> "invalid_request";
            case OAUTH2_INVALID_CLIENT, CLIENT_NOT_FOUND, CLIENT_AUTHENTICATION_FAILED -> "invalid_client";
            case OAUTH2_INVALID_GRANT, AUTHORIZATION_CODE_INVALID, AUTHORIZATION_CODE_EXPIRED,
                 AUTHORIZATION_CODE_USED, REFRESH_TOKEN_INVALID, REFRESH_TOKEN_EXPIRED -> "invalid_grant";
            case OAUTH2_UNAUTHORIZED_CLIENT -> "unauthorized_client";
            case OAUTH2_UNSUPPORTED_GRANT_TYPE -> "unsupported_grant_type";
            case OAUTH2_INVALID_SCOPE -> "invalid_scope";
            case OAUTH2_ACCESS_DENIED -> "access_denied";
            case OAUTH2_SERVER_ERROR, TOKEN_GENERATION_FAILED, JWT_GENERATION_FAILED -> "server_error";
            default -> "server_error";
        };
    }
}
