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
@Document(indexName = "category_index")
@Setting(settingPath = "elasticsearch/category-settings.json")
@Mapping(mappingPath = "elasticsearch/category-mapping.json")
public class CategoryDocument {

    


    @Id
    private String id;

    


    @Field(type = FieldType.Long)
    private Long categoryId;

    


    @Field(type = FieldType.Long)
    private Long parentId;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String categoryName;

    


    @Field(type = FieldType.Integer)
    private Integer level;

    


    @Field(type = FieldType.Integer)
    private Integer status;

    


    @Field(type = FieldType.Keyword)
    private String icon;

    


    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    


    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    


    @Field(type = FieldType.Integer)
    private Integer productCount;

    


    @Field(type = FieldType.Keyword)
    private String categoryPath;

    


    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    


    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    


    @Field(type = FieldType.Double)
    private Double searchWeight;

    


    @Field(type = FieldType.Double)
    private Double hotScore;
}
