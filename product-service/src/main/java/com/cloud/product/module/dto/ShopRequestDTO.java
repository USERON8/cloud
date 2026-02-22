package com.cloud.product.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;







@Data
@Schema(description = "搴楅摵璇锋眰DTO")
public class ShopRequestDTO {

    @Schema(description = "鍟嗗ID", required = true)
    @NotNull(message = "鍟嗗ID涓嶈兘涓虹┖")
    private Long merchantId;

    @Schema(description = "搴楅摵鍚嶇О", required = true)
    @NotBlank(message = "搴楅摵鍚嶇О涓嶈兘涓虹┖")
    @Size(max = 100, message = "搴楅摵鍚嶇О涓嶈兘瓒呰繃100涓瓧绗?)
    private String shopName;

    @Schema(description = "搴楅摵澶村儚URL")
    @Size(max = 500, message = "搴楅摵澶村儚URL涓嶈兘瓒呰繃500涓瓧绗?)
    private String avatarUrl;

    @Schema(description = "搴楅摵鎻忚堪")
    @Size(max = 500, message = "搴楅摵鎻忚堪涓嶈兘瓒呰繃500涓瓧绗?)
    private String description;

    @Schema(description = "瀹㈡湇鐢佃瘽")
    @Pattern(regexp = "^[0-9-()\\s+]{0,20}$", message = "瀹㈡湇鐢佃瘽鏍煎紡涓嶆纭?)
    private String contactPhone;

    @Schema(description = "璇︾粏鍦板潃")
    @Size(max = 200, message = "璇︾粏鍦板潃涓嶈兘瓒呰繃200涓瓧绗?)
    private String address;

    @Schema(description = "搴楅摵鐘舵€侊細0-鍏抽棴锛?-钀ヤ笟", allowableValues = {"0", "1"})
    private Integer status;

    
    public String getName() {
        return this.shopName;
    }

    public void setName(String name) {
        this.shopName = name;
    }
}
