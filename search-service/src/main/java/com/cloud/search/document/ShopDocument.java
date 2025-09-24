package com.cloud.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * 店铺搜索文档
 * 对应Elasticsearch中的店铺索引
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "shop_index")
@Setting(settingPath = "elasticsearch/shop-settings.json")
@Mapping(mappingPath = "elasticsearch/shop-mapping.json")
public class ShopDocument {

    /**
     * 店铺ID（作为ES文档ID）
     */
    @Id
    private String id;

    /**
     * 店铺ID
     */
    @Field(type = FieldType.Long)
    private Long shopId;

    /**
     * 商家ID
     */
    @Field(type = FieldType.Long)
    private Long merchantId;

    /**
     * 店铺名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String shopName;

    /**
     * 店铺头像URL
     */
    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    /**
     * 店铺描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 客服电话
     */
    @Field(type = FieldType.Keyword)
    private String contactPhone;

    /**
     * 详细地址
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    /**
     * 状态：0-关闭，1-营业
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 商品数量
     */
    @Field(type = FieldType.Integer)
    private Integer productCount;

    /**
     * 店铺评分
     */
    @Field(type = FieldType.Double)
    private Double rating;

    /**
     * 评价数量
     */
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    /**
     * 关注数量
     */
    @Field(type = FieldType.Integer)
    private Integer followCount;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    /**
     * 搜索权重
     */
    @Field(type = FieldType.Double)
    private Double searchWeight;

    /**
     * 热度分数
     */
    @Field(type = FieldType.Double)
    private Double hotScore;

    /**
     * 是否推荐店铺
     */
    @Field(type = FieldType.Boolean)
    private Boolean recommended;
}
