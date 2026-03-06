package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;








@EqualsAndHashCode(callSuper = true)
@TableName(value = "brand_authorization")
@Data
public class BrandAuthorization extends BaseEntity<BrandAuthorization> {
    


    @TableField(value = "brand_id")
    private Long brandId;

    


    @TableField(value = "brand_name")
    private String brandName;

    


    @TableField(value = "merchant_id")
    private Long merchantId;

    


    @TableField(value = "merchant_name")
    private String merchantName;

    


    @TableField(value = "auth_type")
    private String authType;

    


    @TableField(value = "auth_status")
    private String authStatus;

    


    @TableField(value = "certificate_url")
    private String certificateUrl;

    


    @TableField(value = "start_time")
    private LocalDateTime startTime;

    


    @TableField(value = "end_time")
    private LocalDateTime endTime;

    


    @TableField(value = "auditor_id")
    private Long auditorId;

    


    @TableField(value = "auditor_name")
    private String auditorName;

    


    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    


    @TableField(value = "audit_comment")
    private String auditComment;

    


    @TableField(value = "remark")
    private String remark;
}
