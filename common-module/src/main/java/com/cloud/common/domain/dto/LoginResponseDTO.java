package com.cloud.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private long expiresIn;
    private String userType;    // 用户类型（ADMIN/MERCHANT/USER）
    private String nickname;    // 昵称（消费者）或店铺名（商家）
}
