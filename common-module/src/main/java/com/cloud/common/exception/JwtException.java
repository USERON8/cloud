package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * JWT令牌相关异常类
 * 用于处理JWT令牌的解析、验证、生成等操作中的异常
 *
 * @author what's up
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JwtException extends BusinessException {

    private String tokenValue;  // 令牌值（敏感信息，可能需要脱敏）
    private String tokenType;   // 令牌类型（access_token, refresh_token, id_token等）

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

    /**
     * 创建JWT令牌无效异常
     */
    public static JwtException invalidToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_INVALID, message);
    }

    /**
     * 创建JWT令牌过期异常
     */
    public static JwtException expiredToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_EXPIRED, message);
    }

    /**
     * 创建JWT令牌格式错误异常
     */
    public static JwtException malformedToken(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_MALFORMED, message);
    }

    /**
     * 创建JWT签名验证失败异常
     */
    public static JwtException invalidSignature(String message) {
        return new JwtException(ResultCode.JWT_SIGNATURE_INVALID, message);
    }

    /**
     * 创建JWT令牌未找到异常
     */
    public static JwtException tokenNotFound(String message) {
        return new JwtException(ResultCode.JWT_TOKEN_NOT_FOUND, message);
    }

    /**
     * 创建JWT令牌生成失败异常
     */
    public static JwtException generationFailed(String message) {
        return new JwtException(ResultCode.JWT_GENERATION_FAILED, message);
    }

    /**
     * 创建JWT令牌生成失败异常（带原因）
     */
    public static JwtException generationFailed(String message, Throwable cause) {
        return new JwtException(message, cause);
    }

    /**
     * 脱敏令牌值，只显示前8位和后4位
     */
    private String maskSensitiveToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "***" + token.substring(token.length() - 4);
    }
}
