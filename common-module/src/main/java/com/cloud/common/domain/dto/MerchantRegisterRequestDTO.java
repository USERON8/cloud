package com.cloud.common.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 商家注册请求DTO
 */
@Data
public class MerchantRegisterRequestDTO {
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 昵称/店铺名称
     */
    @NotBlank(message = "店铺名称不能为空")
    private String nickname;

    /**
     * 营业执照号码
     */
    @NotBlank(message = "营业执照号码不能为空")
    private String businessLicenseNumber;

    /**
     * 营业执照图片文件
     */
    @NotNull(message = "营业执照图片不能为空")
    private MultipartFile businessLicenseFile;

    /**
     * 身份证正面图片文件
     */
    @NotNull(message = "身份证正面图片不能为空")
    private MultipartFile idCardFrontFile;

    /**
     * 身份证反面图片文件
     */
    @NotNull(message = "身份证反面图片不能为空")
    private MultipartFile idCardBackFile;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /**
     * 联系地址
     */
    @NotBlank(message = "联系地址不能为空")
    private String contactAddress;
}