package com.cloud.product.module.dto;

import com.cloud.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Shop page query DTO")
public class ShopPageDTO extends PageQuery {

    @Schema(description = "Merchant id")
    private Long merchantId;

    @Schema(description = "Shop name keyword")
    private String shopNameKeyword;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Address keyword")
    private String addressKeyword;

    @Schema(description = "Create time sort")
    private String createTimeSort;

    @Schema(description = "Update time sort")
    private String updateTimeSort;

    public String getName() {
        return this.shopNameKeyword;
    }

    public void setName(String name) {
        this.shopNameKeyword = name;
    }
}
