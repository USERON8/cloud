package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

















@Data
@EqualsAndHashCode(callSuper = true)
public class OAuth2Exception extends BusinessException {

    private String error;        
    private String errorDescription; 
    private String errorUri;     
    private String state;        

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

    


    public static OAuth2Exception invalidRequest(String description) {
        return new OAuth2Exception("invalid_request", description);
    }

    


    public static OAuth2Exception invalidClient(String description) {
        return new OAuth2Exception("invalid_client", description);
    }

    


    public static OAuth2Exception invalidGrant(String description) {
        return new OAuth2Exception("invalid_grant", description);
    }

    


    public static OAuth2Exception unauthorizedClient(String description) {
        return new OAuth2Exception("unauthorized_client", description);
    }

    


    public static OAuth2Exception unsupportedGrantType(String description) {
        return new OAuth2Exception("unsupported_grant_type", description);
    }

    


    public static OAuth2Exception invalidScope(String description) {
        return new OAuth2Exception("invalid_scope", description);
    }

    


    public static OAuth2Exception accessDenied(String description) {
        return new OAuth2Exception("access_denied", description);
    }

    


    public static OAuth2Exception serverError(String description) {
        return new OAuth2Exception("server_error", description);
    }

    


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
