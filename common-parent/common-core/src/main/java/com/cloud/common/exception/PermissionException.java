package com.cloud.common.exception;

public class PermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final Object[] args;

    public PermissionException() {
        super("Permission denied");
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

    public static PermissionException insufficientPermission(String requiredPermission) {
        return new PermissionException(
                "INSUFFICIENT_PERMISSION",
                String.format("Required permission %s is missing", requiredPermission),
                new Object[]{requiredPermission}
        );
    }

    public static PermissionException insufficientScope(String[] requiredScopes) {
        return new PermissionException(
                "INSUFFICIENT_SCOPE",
                String.format("Required scopes %s are missing", String.join(", ", requiredScopes)),
                new Object[]{requiredScopes}
        );
    }

    public static PermissionException roleMismatch(String requiredRole, String currentRole) {
        return new PermissionException(
                "ROLE_MISMATCH",
                String.format("Required role %s but current role is %s", requiredRole, currentRole),
                new Object[]{requiredRole, currentRole}
        );
    }

    public static PermissionException notAuthenticated() {
        return new PermissionException(
                "NOT_AUTHENTICATED",
                "Authentication is required"
        );
    }

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
