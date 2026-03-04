package com.cloud.user.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_favorite")
public class UserFavoriteV2 extends BaseEntity<UserFavoriteV2> {

    @TableField("user_id")
    private Long userId;
    @TableField("spu_id")
    private Long spuId;
    @TableField("sku_id")
    private Long skuId;
    @TableField("favorite_status")
    private String favoriteStatus;
}

