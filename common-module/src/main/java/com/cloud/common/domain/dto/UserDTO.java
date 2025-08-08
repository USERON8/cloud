package com.cloud.common.domain.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;            // 用户ID（继承自BaseEntity）
    private String username;    // 用户名
    private String userType;    // 用户类型（admin/merchant/user）
    private String email;       // 邮箱
    private String phone;       // 手机号
    private String nickname;    // 昵称
    private String avatarUrl;   // 头像URL
    private Integer status;     // 状态（0禁用，1启用）
    
}