package com.cloud.product.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺请求DTO
 * 用于店铺创建和更新操作
 *
 * @author what's up
 */
@Data
@Schema(description = "店铺请求DTO")
public class ShopRequestDTO {

    @Schema(description = "商家ID", required = true)
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @Schema(description = "店铺名称", required = true)
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称不能超过100个字符")
    private String shopName;

    @Schema(description = "店铺头像URL")
    @Size(max = 500, message = "店铺头像URL不能超过500个字符")
    private String avatarUrl;

    @Schema(description = "店铺描述")
    @Size(max = 500, message = "店铺描述不能超过500个字符")
    private String description;

    @Schema(description = "客服电话")
    @Pattern(regexp = "^[0-9-()\\s+]{0,20}$", message = "客服电话格式不正确")
    private String contactPhone;

    @Schema(description = "详细地址")
    @Size(max = 200, message = "详细地址不能超过200个字符")
    private String address;

    @Schema(description = "店铺状态：0-关闭，1-营业", allowableValues = {"0", "1"})
    private Integer status;
    
    // 为了兼容性提供getName方法
    public String getName() {
        return this.shopName;
    }
    
    public void setName(String name) {
        this.shopName = name;
    }
}
