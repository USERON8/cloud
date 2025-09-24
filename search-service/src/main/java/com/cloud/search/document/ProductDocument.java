package com.cloud.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索文档
 * 对应Elasticsearch中的商品索引
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "product_index")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Mapping(mappingPath = "elasticsearch/product-mapping.json")
public class ProductDocument {

    /**
     * 商品ID（作为ES文档ID）
     */
    @Id
    private String id;

    /**
     * 商品ID
     */
    @Field(type = FieldType.Long)
    private Long productId;

    /**
     * 店铺ID
     */
    @Field(type = FieldType.Long)
    private Long shopId;

    /**
     * 店铺名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String shopName;

    /**
     * 商品名称（支持中文分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    /**
     * 商品价格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    /**
     * 分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 分类名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;

    /**
     * 品牌ID
     */
    @Field(type = FieldType.Long)
    private Long brandId;

    /**
     * 品牌名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String brandName;

    /**
     * 商品状态：0-下架，1-上架
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 商品描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 商品图片URL
     */
    @Field(type = FieldType.Keyword)
    private String imageUrl;

    /**
     * 商品标签（用于搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String tags;

    /**
     * 销量
     */
    @Field(type = FieldType.Integer)
    private Integer salesCount;

    /**
     * 评分
     */
    @Field(type = FieldType.Double)
    private BigDecimal rating;

    /**
     * 评价数量
     */
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

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
     * 搜索权重（用于排序）
     */
    @Field(type = FieldType.Double)
    private Double searchWeight;

    /**
     * 热度分数（综合销量、评分、点击等）
     */
    @Field(type = FieldType.Double)
    private Double hotScore;

    /**
     * 是否推荐
     */
    @Field(type = FieldType.Boolean)
    private Boolean recommended;

    /**
     * 是否新品
     */
    @Field(type = FieldType.Boolean)
    private Boolean isNew;

    /**
     * 是否热销
     */
    @Field(type = FieldType.Boolean)
    private Boolean isHot;
}
