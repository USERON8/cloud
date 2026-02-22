package com.cloud.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;









@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "product_index")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Mapping(mappingPath = "elasticsearch/product-mapping.json")
public class ProductDocument {

    


    @Id
    private String id;

    


    @Field(type = FieldType.Long)
    private Long productId;

    


    @Field(type = FieldType.Long)
    private Long shopId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String shopName;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String productName;

    


    @Field(type = FieldType.Keyword)
    private String productNameKeyword;

    


    @Field(type = FieldType.Double)
    private BigDecimal price;

    


    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    


    @Field(type = FieldType.Long)
    private Long categoryId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String categoryName;

    


    @Field(type = FieldType.Keyword)
    private String categoryNameKeyword;

    


    @Field(type = FieldType.Long)
    private Long brandId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String brandName;

    


    @Field(type = FieldType.Keyword)
    private String brandNameKeyword;

    


    @Field(type = FieldType.Integer)
    private Integer status;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    


    @Field(type = FieldType.Keyword)
    private String imageUrl;

    


    @Field(type = FieldType.Keyword)
    private String detailImages;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;

    


    @Field(type = FieldType.Double)
    private BigDecimal weight;

    


    @Field(type = FieldType.Keyword)
    private String sku;

    


    @Field(type = FieldType.Integer)
    private Integer salesCount;

    


    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    


    @Field(type = FieldType.Double)
    private BigDecimal rating;

    


    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    


    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    


    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    


    @Field(type = FieldType.Double)
    private Double searchWeight;

    


    @Field(type = FieldType.Double)
    private Double hotScore;

    


    @Field(type = FieldType.Boolean)
    private Boolean recommended;

    


    @Field(type = FieldType.Boolean)
    private Boolean isNew;

    


    @Field(type = FieldType.Boolean)
    private Boolean isHot;

    


    @Field(type = FieldType.Long)
    private Long merchantId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String merchantName;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String remark;
}
