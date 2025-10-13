package com.cloud.user.module.enums;

/**
 * 用户活动类型枚举
 *
 * @author what's up
 */
public enum UserActivityType {
    /**
     * 登录
     */
    LOGIN("登录"),

    /**
     * 登出
     */
    LOGOUT("登出"),

    /**
     * 注册
     */
    REGISTRATION("注册"),

    /**
     * 个人信息修改
     */
    PROFILE_UPDATE("个人信息修改"),

    /**
     * 密码修改
     */
    PASSWORD_CHANGE("密码修改"),

    /**
     * 密码重置
     */
    PASSWORD_RESET("密码重置"),

    /**
     * 账户激活
     */
    ACCOUNT_ACTIVATION("账户激活"),

    /**
     * 账户禁用
     */
    ACCOUNT_DISABLED("账户禁用"),

    /**
     * 浏览
     */
    VIEW("浏览"),

    /**
     * 搜索
     */
    SEARCH("搜索"),

    /**
     * 其他
     */
    OTHER("其他");

    private final String description;

    UserActivityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
