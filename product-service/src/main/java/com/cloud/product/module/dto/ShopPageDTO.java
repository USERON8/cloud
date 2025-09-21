package com.cloud.product.module.dto;

import com.cloud.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺分页查询DTO
 * 用于店铺分页查询操作
 *
 * @author what's up
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "店铺分页查询DTO")
public class ShopPageDTO extends PageQuery {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "店铺名称关键词")
    private String shopNameKeyword;

    @Schema(description = "店铺状态：0-关闭，1-营业")
    private Integer status;

    @Schema(description = "地址关键词")
    private String addressKeyword;

    @Schema(description = "创建时间排序：ASC-升序，DESC-降序")
    private String createTimeSort;

    @Schema(description = "更新时间排序：ASC-升序，DESC-降序")
    private String updateTimeSort;

    // 为了兼容性提供getName方法（返回shopNameKeyword）
    public String getName() {
        return this.shopNameKeyword;
    }

    public void setName(String name) {
        this.shopNameKeyword = name;
    }
}
