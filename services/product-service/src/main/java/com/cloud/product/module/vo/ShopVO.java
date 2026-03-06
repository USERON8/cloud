package com.cloud.product.module.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Shop view object")
public class ShopVO {

    @Schema(description = "Shop id")
    private Long id;

    @Schema(description = "Merchant id")
    private Long merchantId;

    @Schema(description = "Shop name")
    private String shopName;

    @Schema(description = "Avatar url")
    private String avatarUrl;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Contact phone")
    private String contactPhone;

    @Schema(description = "Address")
    private String address;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Status description")
    private String statusDesc;

    @Schema(description = "Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "Created by")
    private Long createBy;

    @Schema(description = "Updated by")
    private Long updateBy;

    @Schema(description = "Product count")
    private Long productCount;

    @Schema(description = "Owner flag")
    private Boolean isOwner;

    public String getName() {
        return this.shopName;
    }

    public void setName(String name) {
        this.shopName = name;
    }
}
