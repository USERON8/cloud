package com.cloud.common.exception;

/**
 * 权限异常类
 * 当用户权限不足时抛出此异常
 *
 * @author what's up
 * @date 2024-01-20
 */
public class PermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final Object[] args;

    public PermissionException() {
        super("权限不足");
        this.code = "PERMISSION_DENIED";
        this.args = null;
    }

    public PermissionException(String message) {
        super(message);
        this.code = "PERMISSION_DENIED";
        this.args = null;
    }

    public PermissionException(String code, String message) {
        super(message);
        this.code = code;
        this.args = null;
    }

    public PermissionException(String message, Throwable cause) {
        super(message, cause);
        this.code = "PERMISSION_DENIED";
        this.args = null;
    }

    public PermissionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = null;
    }

    public PermissionException(String code, String message, Object[] args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    /**
     * 创建权限不足异常
     *
     * @param requiredPermission 所需权限
     * @return 权限异常
     */
    public static PermissionException insufficientPermission(String requiredPermission) {
        return new PermissionException(
                "INSUFFICIENT_PERMISSION",
                String.format("权限不足，需要权限: %s", requiredPermission),
                new Object[]{requiredPermission}
        );
    }

    /**
     * 创建权限范围不足异常
     *
     * @param requiredScopes 所需权限范围
     * @return 权限异常
     */
    public static PermissionException insufficientScope(String[] requiredScopes) {
        return new PermissionException(
                "INSUFFICIENT_SCOPE",
                String.format("权限范围不足，需要权限: %s", String.join(", ", requiredScopes)),
                new Object[]{requiredScopes}
        );
    }

    /**
     * 创建用户类型不匹配异常
     *
     * @param requiredUserType 所需用户类型
     * @param currentUserType  当前用户类型
     * @return 权限异常
     */
    public static PermissionException userTypeMismatch(String requiredUserType, String currentUserType) {
        return new PermissionException(
                "USER_TYPE_MISMATCH",
                String.format("用户类型不匹配，需要: %s，当前: %s", requiredUserType, currentUserType),
                new Object[]{requiredUserType, currentUserType}
        );
    }

    /**
     * 创建未认证异常
     *
     * @return 权限异常
     */
    public static PermissionException notAuthenticated() {
        return new PermissionException(
                "NOT_AUTHENTICATED",
                "用户未认证，请先登录"
        );
    }

    /**
     * 创建未认证异常
     *
     * @param message 自定义消息
     * @return 权限异常
     */
    public static PermissionException notAuthenticated(String message) {
        return new PermissionException(
                "NOT_AUTHENTICATED",
                message
        );
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
