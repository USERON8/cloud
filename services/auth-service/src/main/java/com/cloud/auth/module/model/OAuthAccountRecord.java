package com.cloud.auth.module.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccountRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String provider;

    private String providerUserId;

    private Long userId;

    private String providerUsername;

    private String email;

    private String avatarUrl;

    private Long lastSyncAt;
}
