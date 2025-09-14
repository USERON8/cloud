package com.cloud.product.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品分页查询DTO
 *
 * @author what's up
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageDTO extends PageQuery {
    /**
     * 商品名称（模糊查询）
     */
    private String name;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称（模糊查询）
     */
    private String categoryName;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称（模糊查询）
     */
    private String brandName;

    /**
     * 商品状态：0-下架，1-上架，null-全部
     */
    private Integer status;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 最低库存
     */
    private Integer minStock;

    /**
     * 最高库存
     */
    private Integer maxStock;

    /**
     * 价格排序：ASC-升序，DESC-降序
     */
    private String priceSort;

    /**
     * 库存排序：ASC-升序，DESC-降序
     */
    private String stockSort;

    /**
     * 销量排序：ASC-升序，DESC-降序
     */
    private String salesSort;

    /**
     * 创建时间排序：ASC-升序，DESC-降序
     */
    private String createTimeSort;
    
    /**
     * 更新时间排序：ASC-升序，DESC-降序
     */
    private String updateTimeSort;
}
