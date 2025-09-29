package com.cloud.common.domain.event.product;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 商品变更日志事件
 * 
 * 记录商品相关的操作日志，包括：
 * - 商品创建、更新、删除
 * - 商品上架、下架
 * - 商品价格变更
 * - 商品库存变更
 * - 商品分类调整等
 * 
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductChangeLogEvent extends BaseBusinessLogEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品SKU
     */
    private String productSku;

    /**
     * 商品分类ID
     */
    private Long categoryId;

    /**
     * 商品分类名称
     */
    private String categoryName;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 价格变更（原价格 -> 新价格）
     */
    private String priceChange;

    /**
     * 库存变更数量
     */
    private Integer stockChange;

    /**
     * 商品状态变更
     * DRAFT -> ACTIVE（草稿->上架）
     * ACTIVE -> INACTIVE（上架->下架）
     * INACTIVE -> DELETED（下架->删除）
     */
    private String statusChange;

    @Override
    public String getLogType() {
        return "PRODUCT_CHANGE";
    }
}
