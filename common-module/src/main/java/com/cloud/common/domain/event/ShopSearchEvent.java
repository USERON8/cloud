package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺搜索事件
 * 用于店铺数据变更时同步到搜索服务的Elasticsearch
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSearchEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型：CREATE, UPDATE, DELETE, STATUS_CHANGE
     */
    private String eventType;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺头像URL
     */
    private String avatarUrl;

    /**
     * 店铺描述
     */
    private String description;

    /**
     * 客服电话
     */
    private String contactPhone;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 状态：0-关闭，1-营业
     */
    private Integer status;

    /**
     * 商品数量
     */
    private Integer productCount;

    /**
     * 店铺评分
     */
    private Double rating;

    /**
     * 评价数量
     */
    private Integer reviewCount;

    /**
     * 关注数量
     */
    private Integer followCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 备注信息
     */
    private String remark;
}
