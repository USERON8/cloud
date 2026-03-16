package com.cloud.product.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_review")
@Data
public class ProductReview extends BaseEntity<ProductReview> {

  @TableField(value = "spu_id")
  private Long spuId;

  @TableField(value = "sku_id")
  private Long skuId;

  @TableField(value = "order_sub_no")
  private String orderSubNo;

  @TableField(value = "user_id")
  private Long userId;

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

  @TableField(value = "merchant_reply")
  private String merchantReply;

  @TableField(value = "reply_time")
  private LocalDateTime replyTime;

  @TableField(value = "like_count")
  private Integer likeCount;

  @TableField(value = "is_visible")
  private Integer isVisible;
}
