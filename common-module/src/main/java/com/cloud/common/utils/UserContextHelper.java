package com.cloud.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户上下文助手类
 * 用于ThreadLocal存储和获取网关转发的用户信息
 * 支持OAuth2.1标准的用户信息传递
 * <p>
 * 注意：这个类依赖网关过滤器在请求处理前设置用户信息
 *
 * @author what's up
 */
@Slf4j
public class UserContextHelper {

    // 用户信息请求头常量
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NICKNAME = "X-User-Nickname";
    public static final String HEADER_USER_STATUS = "X-User-Status";
    public static final String HEADER_USER_PHONE = "X-User-Phone";
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_USER_SCOPES = "X-User-Scopes";
    public static final String HEADER_TOKEN_VERSION = "X-Token-Version";
    // ThreadLocal存储用户上下文
    private static final ThreadLocal<Map<String, String>> userContextHolder =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 设置用户上下文信息（由网关过滤器调用）
     */
    public static void setUserContext(Map<String, String> userInfo) {
        userContextHolder.set(new ConcurrentHashMap<>(userInfo));
    }

    /**
     * 清理当前线程的用户上下文（请求结束后调用）
     */
    public static void clearUserContext() {
        userContextHolder.remove();
    }

    /**
     * 获取当前请求的用户名
     */
    public static String getCurrentUsername() {
        return getHeaderValue(HEADER_USER_NAME);
    }

    /**
     * 获取当前请求的用户类型
     */
    public static String getCurrentUserType() {
        return getHeaderValue(HEADER_USER_TYPE);
    }

    /**
     * 获取当前请求的用户ID
     */
    public static Long getCurrentUserId() {
        String userId = getHeaderValue(HEADER_USER_ID);
        if (StringUtils.hasText(userId) && !"null".equals(userId)) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                log.warn("用户ID格式不正确: {}", userId);
            }
        }
        return null;
    }

    /**
     * 获取当前请求的用户昵称
     */
    public static String getCurrentUserNickname() {
        return getHeaderValue(HEADER_USER_NICKNAME);
    }

    /**
     * 获取当前请求的用户状态
     */
    public static Integer getCurrentUserStatus() {
        String status = getHeaderValue(HEADER_USER_STATUS);
        if (StringUtils.hasText(status) && !"null".equals(status)) {
            try {
                return Integer.parseInt(status);
            } catch (NumberFormatException e) {
                log.warn("用户状态格式不正确: {}", status);
            }
        }
        return null;
    }

    /**
     * 获取当前请求的用户手机号（脱敏后）
     */
    public static String getCurrentUserPhone() {
        return getHeaderValue(HEADER_USER_PHONE);
    }

    /**
     * 获取当前请求的客户端ID
     */
    public static String getCurrentClientId() {
        return getHeaderValue(HEADER_CLIENT_ID);
    }

    /**
     * 获取当前请求的用户权限范围
     */
    public static String getCurrentUserScopes() {
        return getHeaderValue(HEADER_USER_SCOPES);
    }

    /**
     * 获取Token版本
     */
    public static String getTokenVersion() {
        return getHeaderValue(HEADER_TOKEN_VERSION);
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isCurrentUserAdmin() {
        String userType = getCurrentUserType();
        return "ADMIN".equalsIgnoreCase(userType);
    }

    /**
     * 检查当前用户是否为商户
     */
    public static boolean isCurrentUserMerchant() {
        String userType = getCurrentUserType();
        return "MERCHANT".equalsIgnoreCase(userType);
    }

    /**
     * 检查当前用户是否为普通用户
     */
    public static boolean isCurrentUserNormal() {
        String userType = getCurrentUserType();
        return userType == null || "USER".equalsIgnoreCase(userType);
    }

    /**
     * 检查当前用户是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        String scopes = getCurrentUserScopes();
        if (!StringUtils.hasText(scopes) || !StringUtils.hasText(permission)) {
            return false;
        }
        return scopes.contains(permission);
    }

    /**
     * 获取完整的用户上下文信息
     */
    public static UserContext getCurrentUserContext() {
        return UserContext.builder()
                .username(getCurrentUsername())
                .userType(getCurrentUserType())
                .userId(getCurrentUserId())
                .nickname(getCurrentUserNickname())
                .status(getCurrentUserStatus())
                .phone(getCurrentUserPhone())
                .clientId(getCurrentClientId())
                .scopes(getCurrentUserScopes())
                .tokenVersion(getTokenVersion())
                .build();
    }

    /**
     * 从 ThreadLocal 中获取用户信息
     */
    private static String getHeaderValue(String headerName) {
        try {
            Map<String, String> userContext = userContextHolder.get();
            if (userContext != null && userContext.containsKey(headerName)) {
                String value = userContext.get(headerName);
                // 过滤掉"null"字符串和空值
                if (StringUtils.hasText(value) && !"null".equals(value)) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.debug("获取用户信息 {} 时发生异常: {}", headerName, e.getMessage());
        }
        return null;
    }

    /**
     * 用户上下文信息类
     */
    public static class UserContext {
        private final String username;
        private final String userType;
        private final Long userId;
        private final String nickname;
        private final Integer status;
        private final String phone;
        private final String clientId;
        private final String scopes;
        private final String tokenVersion;

        // 构造器
        private UserContext(Builder builder) {
            this.username = builder.username;
            this.userType = builder.userType;
            this.userId = builder.userId;
            this.nickname = builder.nickname;
            this.status = builder.status;
            this.phone = builder.phone;
            this.clientId = builder.clientId;
            this.scopes = builder.scopes;
            this.tokenVersion = builder.tokenVersion;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getUsername() {
            return username;
        }

        public String getUserType() {
            return userType;
        }

        public Long getUserId() {
            return userId;
        }

        public String getNickname() {
            return nickname;
        }

        public Integer getStatus() {
            return status;
        }

        public String getPhone() {
            return phone;
        }

        public String getClientId() {
            return clientId;
        }

        public String getScopes() {
            return scopes;
        }

        public String getTokenVersion() {
            return tokenVersion;
        }

        @Override
        public String toString() {
            return "UserContext{" +
                    "username='" + username + '\'' +
                    ", userType='" + userType + '\'' +
                    ", userId=" + userId +
                    ", nickname='" + nickname + '\'' +
                    ", status=" + status +
                    ", phone='" + phone + '\'' +
                    ", clientId='" + clientId + '\'' +
                    ", scopes='" + scopes + '\'' +
                    ", tokenVersion='" + tokenVersion + '\'' +
                    '}';
        }

        // Builder类
        public static class Builder {
            private String username;
            private String userType;
            private Long userId;
            private String nickname;
            private Integer status;
            private String phone;
            private String clientId;
            private String scopes;
            private String tokenVersion;

            public Builder username(String username) {
                this.username = username;
                return this;
            }

            public Builder userType(String userType) {
                this.userType = userType;
                return this;
            }

            public Builder userId(Long userId) {
                this.userId = userId;
                return this;
            }

            public Builder nickname(String nickname) {
                this.nickname = nickname;
                return this;
            }

            public Builder status(Integer status) {
                this.status = status;
                return this;
            }

            public Builder phone(String phone) {
                this.phone = phone;
                return this;
            }

            public Builder clientId(String clientId) {
                this.clientId = clientId;
                return this;
            }

            public Builder scopes(String scopes) {
                this.scopes = scopes;
                return this;
            }

            public Builder tokenVersion(String tokenVersion) {
                this.tokenVersion = tokenVersion;
                return this;
            }

            public UserContext build() {
                return new UserContext(this);
            }
        }
    }
}
