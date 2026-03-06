package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;







@Data
@EqualsAndHashCode(callSuper = true)
public class JwtException extends BusinessException {

    private String tokenValue;  
    private String tokenType;   

    public JwtException(String message) {
        super(message);
    }

    public JwtException(ResultCode resultCode) {
        super(resultCode);
    }

    public JwtException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public JwtException(String message, String tokenType) {
        super(message);
        this.tokenType = tokenType;
    }

    public JwtException(ResultCode resultCode, String tokenType, String tokenValue) {
        super(resultCode);
        this.tokenType = tokenType;
        this.tokenValue = maskSensitiveToken(tokenValue);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    


    public static JwtException invalidToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_INVALID, message);
    }

    


    public static JwtException expiredToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_EXPIRED, message);
    }

    


    public static JwtException malformedToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_MALFORMED, message);
    }

    


    public static JwtException invalidSignature(String message) {
        return new JwtException(ResultCode.JWT_SIGNATURE_INVALID, message);
    }

    


    public static JwtException tokenNotFound(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_NOT_FOUND, message);
    }

    


    public static JwtException generationFailed(String message) {
        return new JwtException(ResultCode.JWT_GENERATION_FAILED, message);
    }

    


    public static JwtException generationFailed(String message, Throwable cause) {
        return new JwtException(message, cause);
    }

    


    private String maskSensitiveToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "***" + token.substring(token.length() - 4);
    }
}
