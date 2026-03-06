package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "merchant_auth")
@Data
public class MerchantAuth extends BaseEntity<MerchantAuth> {
    


    @TableField(value = "merchant_id")
    private Long merchantId;

    


    @TableField(value = "business_license_number")
    private String businessLicenseNumber;

    


    @TableField(value = "business_license_url")
    private String businessLicenseUrl;

    


    @TableField(value = "id_card_front_url")
    private String idCardFrontUrl;

    


    @TableField(value = "id_card_back_url")
    private String idCardBackUrl;

    


    @TableField(value = "contact_phone")
    private String contactPhone;

    


    @TableField(value = "contact_address")
    private String contactAddress;

    


    @TableField(value = "auth_status")
    private Integer authStatus;

    


    @TableField(value = "auth_remark")
    private String authRemark;
}
