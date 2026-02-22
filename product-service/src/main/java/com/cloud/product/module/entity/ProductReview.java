package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;







@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_review")
@Data
public class ProductReview extends BaseEntity<ProductReview> {
    


    @TableField(value = "product_id")
    private Long productId;

    


    @TableField(value = "product_name")
    private String productName;

    


    @TableField(value = "sku_id")
    private Long skuId;

    


    @TableField(value = "order_id")
    private Long orderId;

    


    @TableField(value = "order_no")
    private String orderNo;

    


    @TableField(value = "user_id")
    private Long userId;

    


    @TableField(value = "user_nickname")
    private String userNickname;

    


    @TableField(value = "user_avatar")
    private String userAvatar;

    


    @TableField(value = "rating")
    private Integer rating;

    


    @TableField(value = "content")
    private String content;

    


    @TableField(value = "images")
    private String images;

    


    @TableField(value = "tags")
    private String tags;

    


    @TableField(value = "is_anonymous")
    private Integer isAnonymous;

    


    @TableField(value = "audit_status")
    private String auditStatus;

    


    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    


    @TableField(value = "audit_comment")
    private String auditComment;

    


    @TableField(value = "merchant_reply")
    private String merchantReply;

    


    @TableField(value = "reply_time")
    private LocalDateTime replyTime;

    


    @TableField(value = "like_count")
    private Integer likeCount;

    


    @TableField(value = "is_visible")
    private Integer isVisible;

    


    @TableField(value = "review_type")
    private String reviewType;

    


    @TableField(value = "parent_review_id")
    private Long parentReviewId;
}
