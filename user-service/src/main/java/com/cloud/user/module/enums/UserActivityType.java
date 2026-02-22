package com.cloud.user.module.enums;

public enum UserActivityType {
    LOGIN("Login"),
    LOGOUT("Logout"),
    REGISTRATION("Registration"),
    PROFILE_UPDATE("Profile update"),
    PASSWORD_CHANGE("Password change"),
    PASSWORD_RESET("Password reset"),
    ACCOUNT_ACTIVATION("Account activation"),
    ACCOUNT_DISABLED("Account disabled"),
    VIEW("View"),
    SEARCH("Search"),
    OTHER("Other");

    private final String description;

    UserActivityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}