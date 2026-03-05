package com.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {

    USER("USER", "Regular User"),
    MERCHANT("MERCHANT", "Merchant"),
    ADMIN("ADMIN", "Administrator");

    private final String code;
    private final String name;

    public static UserType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserType userType : UserType.values()) {
            if (userType.getCode().equals(code)) {
                return userType;
            }
        }
        return null;
    }

    public static UserType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (UserType userType : UserType.values()) {
            if (userType.getName().equals(name)) {
                return userType;
            }
        }
        return null;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isMerchant() {
        return this == MERCHANT;
    }

    public boolean isUser() {
        return this == USER;
    }
}
