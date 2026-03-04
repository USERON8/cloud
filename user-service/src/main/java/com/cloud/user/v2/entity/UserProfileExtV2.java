package com.cloud.user.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_profile_ext")
public class UserProfileExtV2 extends BaseEntity<UserProfileExtV2> {

    @TableField("user_id")
    private Long userId;
    @TableField("gender")
    private String gender;
    @TableField("birthday")
    private LocalDate birthday;
    @TableField("bio")
    private String bio;
    @TableField("country")
    private String country;
    @TableField("province")
    private String province;
    @TableField("city")
    private String city;
    @TableField("personal_tags")
    private String personalTags;
    @TableField("preferences")
    private String preferences;
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}

