package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品评价表
 *
 * @author what's up
 * @TableName product_review
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_review")
@Data
public class ProductReview extends BaseEntity<ProductReview> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品名称 (冗余字段)
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * SKU ID
     */
    @TableField(value = "sku_id")
    private Long skuId;

    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 订单号
     */
    @TableField(value = "order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 用户昵称
     */
    @TableField(value = "user_nickname")
    private String userNickname;

    /**
     * 用户头像
     */
    @TableField(value = "user_avatar")
    private String userAvatar;

    /**
     * 评分 (1-5星)
     */
    @TableField(value = "rating")
    private Integer rating;

    /**
     * 评价内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 评价图片 (JSON数组: ["url1","url2","url3"])
     */
    @TableField(value = "images")
    private String images;

    /**
     * 评价标签 (JSON数组: ["质量好","物流快","性价比高"])
     */
    @TableField(value = "tags")
    private String tags;

    /**
     * 是否匿名: 0-否, 1-是
     */
    @TableField(value = "is_anonymous")
    private Integer isAnonymous;

    /**
     * 审核状态: PENDING-待审核, APPROVED-审核通过, REJECTED-审核拒绝
     */
    @TableField(value = "audit_status")
    private String auditStatus;

    /**
     * 审核时间
     */
    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    @TableField(value = "audit_comment")
    private String auditComment;

    /**
     * 商家回复
     */
    @TableField(value = "merchant_reply")
    private String merchantReply;

    /**
     * 商家回复时间
     */
    @TableField(value = "reply_time")
    private LocalDateTime replyTime;

    /**
     * 点赞数
     */
    @TableField(value = "like_count")
    private Integer likeCount;

    /**
     * 是否显示: 0-否, 1-是
     */
    @TableField(value = "is_visible")
    private Integer isVisible;

    /**
     * 评价类型: INITIAL-首次评价, ADDITIONAL-追加评价
     */
    @TableField(value = "review_type")
    private String reviewType;

    /**
     * 父评价ID (追加评价时使用)
     */
    @TableField(value = "parent_review_id")
    private Long parentReviewId;
}
