package com.cloud.common.exception;








public class PermissionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final Object[] args;

    public PermissionException() {
        super("鏉冮檺涓嶈冻");
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
                String.format("鏉冮檺涓嶈冻锛岄渶瑕佹潈闄? %s", requiredPermission),
                new Object[]{requiredPermission}
        );
    }

    





    public static PermissionException insufficientScope(String[] requiredScopes) {
        return new PermissionException(
                "INSUFFICIENT_SCOPE",
                String.format("鏉冮檺鑼冨洿涓嶈冻锛岄渶瑕佹潈闄? %s", String.join(", ", requiredScopes)),
                new Object[]{requiredScopes}
        );
    }

    






    public static PermissionException userTypeMismatch(String requiredUserType, String currentUserType) {
        return new PermissionException(
                "USER_TYPE_MISMATCH",
                String.format("鐢ㄦ埛绫诲瀷涓嶅尮閰嶏紝闇€瑕? %s锛屽綋鍓? %s", requiredUserType, currentUserType),
                new Object[]{requiredUserType, currentUserType}
        );
    }

    




    public static PermissionException notAuthenticated() {
        return new PermissionException(
                "NOT_AUTHENTICATED",
                "鐢ㄦ埛鏈璇侊紝璇峰厛鐧诲綍"
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
