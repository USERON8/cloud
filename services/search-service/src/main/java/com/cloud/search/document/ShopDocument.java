package com.cloud.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;









@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "shop_index")
@Setting(settingPath = "elasticsearch/shop-settings.json")
@Mapping(mappingPath = "elasticsearch/shop-mapping.json")
public class ShopDocument {

    


    @Id
    private String id;

    


    @Field(type = FieldType.Long)
    private Long shopId;

    


    @Field(type = FieldType.Long)
    private Long merchantId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String shopName;

    


    @Field(type = FieldType.Keyword)
    private String shopNameKeyword;

    


    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    


    @Field(type = FieldType.Keyword)
    private String contactPhone;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String address;

    


    @Field(type = FieldType.Integer)
    private Integer status;

    


    @Field(type = FieldType.Integer)
    private Integer productCount;

    


    @Field(type = FieldType.Double)
    private Double rating;

    


    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    


    @Field(type = FieldType.Integer)
    private Integer followCount;

    


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
}
