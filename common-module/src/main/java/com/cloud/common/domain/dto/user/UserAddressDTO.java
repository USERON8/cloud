package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户地址信息DTO
 * 与数据库表user_address字段完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class UserAddressDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地址ID - 对应数据库字段: id
     */
    private Long id;

    /**
     * 用户ID - 对应数据库字段: user_id
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 收货人姓名 - 对应数据库字段: consignee
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过50个字符")
    private String consignee;

    /**
     * 联系电话 - 对应数据库字段: phone
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 省份 - 对应数据库字段: province
     */
    @NotBlank(message = "省份不能为空")
    @Size(max = 20, message = "省份长度不能超过20个字符")
    private String province;

    /**
     * 城市 - 对应数据库字段: city
     */
    @NotBlank(message = "城市不能为空")
    @Size(max = 20, message = "城市长度不能超过20个字符")
    private String city;

    /**
     * 区县 - 对应数据库字段: district
     */
    @NotBlank(message = "区县不能为空")
    @Size(max = 20, message = "区县长度不能超过20个字符")
    private String district;

    /**
     * 街道 - 对应数据库字段: street
     */
    @NotBlank(message = "街道不能为空")
    @Size(max = 100, message = "街道长度不能超过100个字符")
    private String street;

    /**
     * 详细地址 - 对应数据库字段: detail_address
     */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    private String detailAddress;

    /**
     * 是否默认地址 - 对应数据库字段: is_default
     * 0-否，1-是
     */
    @Min(value = 0, message = "默认地址标记值不能小于0")
    @Max(value = 1, message = "默认地址标记值不能大于1")
    private Integer isDefault;

    /**
     * 创建时间 - 对应数据库字段: created_at
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 对应数据库字段: updated_at
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除标记 - 对应数据库字段: deleted
     * 0-未删除，1-已删除
     */
    private Integer deleted;
}