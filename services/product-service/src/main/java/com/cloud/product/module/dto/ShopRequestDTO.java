package com.cloud.product.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Shop request DTO")
public class ShopRequestDTO {

    @Schema(description = "Merchant id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Merchant id cannot be null")
    private Long merchantId;

    @Schema(description = "Shop name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Shop name cannot be blank")
    @Size(max = 100, message = "Shop name length must be <= 100")
    private String shopName;

    @Schema(description = "Avatar url")
    @Size(max = 500, message = "Avatar url length must be <= 500")
    private String avatarUrl;

    @Schema(description = "Description")
    @Size(max = 500, message = "Description length must be <= 500")
    private String description;

    @Schema(description = "Contact phone")
    @Pattern(regexp = "^[0-9-()\\s+]{0,20}$", message = "Invalid contact phone format")
    private String contactPhone;

    @Schema(description = "Address")
    @Size(max = 200, message = "Address length must be <= 200")
    private String address;

    @Schema(description = "Status: 0-disabled, 1-enabled", allowableValues = {"0", "1"})
    private Integer status;

    public String getName() {
        return this.shopName;
    }

    public void setName(String name) {
        this.shopName = name;
    }
}
