package com.cloud.product.module.dto;

import com.cloud.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;







@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "搴楅摵鍒嗛〉鏌ヨDTO")
public class ShopPageDTO extends PageQuery {

    @Schema(description = "鍟嗗ID")
    private Long merchantId;

    @Schema(description = "搴楅摵鍚嶇О鍏抽敭璇?)
    private String shopNameKeyword;

    @Schema(description = "搴楅摵鐘舵€侊細0-鍏抽棴锛?-钀ヤ笟")
    private Integer status;

    @Schema(description = "鍦板潃鍏抽敭璇?)
    private String addressKeyword;

    @Schema(description = "鍒涘缓鏃堕棿鎺掑簭锛欰SC-鍗囧簭锛孌ESC-闄嶅簭")
    private String createTimeSort;

    @Schema(description = "鏇存柊鏃堕棿鎺掑簭锛欰SC-鍗囧簭锛孌ESC-闄嶅簭")
    private String updateTimeSort;

    
    public String getName() {
        return this.shopNameKeyword;
    }

    public void setName(String name) {
        this.shopNameKeyword = name;
    }
}
