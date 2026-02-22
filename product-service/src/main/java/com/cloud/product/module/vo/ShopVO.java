package com.cloud.product.module.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;







@Data
@Schema(description = "搴楅摵瑙嗗浘瀵硅薄")
public class ShopVO {

    @Schema(description = "搴楅摵ID")
    private Long id;

    @Schema(description = "鍟嗗ID")
    private Long merchantId;

    @Schema(description = "搴楅摵鍚嶇О")
    private String shopName;

    @Schema(description = "搴楅摵澶村儚URL")
    private String avatarUrl;

    @Schema(description = "搴楅摵鎻忚堪")
    private String description;

    @Schema(description = "瀹㈡湇鐢佃瘽")
    private String contactPhone;

    @Schema(description = "璇︾粏鍦板潃")
    private String address;

    @Schema(description = "搴楅摵鐘舵€侊細0-鍏抽棴锛?-钀ヤ笟")
    private Integer status;

    @Schema(description = "搴楅摵鐘舵€佹弿杩?)
    private String statusDesc;

    @Schema(description = "鍒涘缓鏃堕棿")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "鏇存柊鏃堕棿")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "鍒涘缓鑰匢D")
    private Long createBy;

    @Schema(description = "鏇存柊鑰匢D")
    private Long updateBy;

    
    @Schema(description = "搴楅摵涓嬬殑鍟嗗搧鏁伴噺")
    private Long productCount;

    @Schema(description = "鏄惁涓哄綋鍓嶇敤鎴风殑搴楅摵")
    private Boolean isOwner;

    
    public String getName() {
        return this.shopName;
    }

    public void setName(String name) {
        this.shopName = name;
    }
}
