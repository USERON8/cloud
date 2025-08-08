package com.cloud.common.enums;

import lombok.Getter;

@Getter
public enum AuthRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
    MERCHANT("ROLE_MERCHANT");
    private final String roleName;

    AuthRole(String roleName) {
        this.roleName = roleName;
    }


}
