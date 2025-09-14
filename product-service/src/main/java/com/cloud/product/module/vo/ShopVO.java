package com.cloud.product.module.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺视图对象
 * 用于店铺信息展示
 *
 * @author what's up
 */
@Data
@Schema(description = "店铺视图对象")
public class ShopVO {

    @Schema(description = "店铺ID")
    private Long id;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺头像URL")
    private String avatarUrl;

    @Schema(description = "店铺描述")
    private String description;

    @Schema(description = "客服电话")
    private String contactPhone;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "店铺状态：0-关闭，1-营业")
    private Integer status;

    @Schema(description = "店铺状态描述")
    private String statusDesc;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "创建者ID")
    private Long createBy;

    @Schema(description = "更新者ID")
    private Long updateBy;

    // 扩展信息
    @Schema(description = "店铺下的商品数量")
    private Long productCount;

    @Schema(description = "是否为当前用户的店铺")
    private Boolean isOwner;
    
    // 为了兼容性提供getName方法
    public String getName() {
        return this.shopName;
    }
    
    public void setName(String name) {
        this.shopName = name;
    }
}
