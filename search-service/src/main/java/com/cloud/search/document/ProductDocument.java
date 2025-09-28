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
     * 商品ID（与数据库product.id对应）
     */
    @Field(type = FieldType.Long)
    private Long productId;

    /**
     * 店铺ID（与数据库product.shop_id对应）
     */
    @Field(type = FieldType.Long)
    private Long shopId;

    /**
     * 店铺名称（与数据库merchant_shop.shop_name对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String shopName;

    /**
     * 商品名称（与数据库product.product_name对应，支持中文分词）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String productName;

    /**
     * 商品名称（用于精确匹配和排序）
     */
    @Field(type = FieldType.Keyword)
    private String productNameKeyword;

    /**
     * 商品价格（与数据库product.price对应）
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 库存数量（与数据库product.stock_quantity对应）
     */
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    /**
     * 分类ID（与数据库product.category_id对应）
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 分类名称（与数据库category.name对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String categoryName;

    /**
     * 分类名称（用于精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private String categoryNameKeyword;

    /**
     * 品牌ID（与数据库product.brand_id对应）
     */
    @Field(type = FieldType.Long)
    private Long brandId;

    /**
     * 品牌名称（与数据库product.brand_name对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String brandName;

    /**
     * 品牌名称（用于精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private String brandNameKeyword;

    /**
     * 商品状态：0-下架，1-上架（与数据库product.status对应）
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 商品描述（与数据库product.description对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    /**
     * 商品图片URL（与数据库product.image_url对应）
     */
    @Field(type = FieldType.Keyword)
    private String imageUrl;

    /**
     * 商品详情图片URLs（与数据库product.detail_images对应）
     */
    @Field(type = FieldType.Keyword)
    private String detailImages;

    /**
     * 商品标签（与数据库product.tags对应，用于搜索）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;

    /**
     * 商品重量（与数据库product.weight对应）
     */
    @Field(type = FieldType.Double)
    private BigDecimal weight;

    /**
     * SKU（与数据库product.sku对应）
     */
    @Field(type = FieldType.Keyword)
    private String sku;

    /**
     * 销量（与数据库product.sales_count对应）
     */
    @Field(type = FieldType.Integer)
    private Integer salesCount;

    /**
     * 排序权重（与数据库product.sort_order对应）
     */
    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    /**
     * 评分（计算得出）
     */
    @Field(type = FieldType.Double)
    private BigDecimal rating;

    /**
     * 评价数量（计算得出）
     */
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    /**
     * 创建时间（与数据库product.created_at对应）
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    /**
     * 更新时间（与数据库product.updated_at对应）
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
     * 是否推荐（与数据库product.is_recommended对应）
     */
    @Field(type = FieldType.Boolean)
    private Boolean recommended;

    /**
     * 是否新品（与数据库product.is_new对应）
     */
    @Field(type = FieldType.Boolean)
    private Boolean isNew;

    /**
     * 是否热销（与数据库product.is_hot对应）
     */
    @Field(type = FieldType.Boolean)
    private Boolean isHot;

    /**
     * 商家ID（与数据库product.merchant_id对应）
     */
    @Field(type = FieldType.Long)
    private Long merchantId;

    /**
     * 商家名称（与数据库product.merchant_name对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String merchantName;

    /**
     * 备注（与数据库product.remark对应）
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String remark;
}
