package com.cloud.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * 分类搜索文档
 * 对应Elasticsearch中的分类索引
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "category_index")
@Setting(settingPath = "elasticsearch/category-settings.json")
@Mapping(mappingPath = "elasticsearch/category-mapping.json")
public class CategoryDocument {

    /**
     * 分类ID（作为ES文档ID）
     */
    @Id
    private String id;

    /**
     * 分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 父分类ID
     */
    @Field(type = FieldType.Long)
    private Long parentId;

    /**
     * 分类名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;

    /**
     * 分类层级
     */
    @Field(type = FieldType.Integer)
    private Integer level;

    /**
     * 状态：0-禁用，1-启用
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 分类图标
     */
    @Field(type = FieldType.Keyword)
    private String icon;

    /**
     * 分类描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 排序值
     */
    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    /**
     * 商品数量
     */
    @Field(type = FieldType.Integer)
    private Integer productCount;

    /**
     * 分类路径（用于层级搜索）
     */
    @Field(type = FieldType.Keyword)
    private String categoryPath;

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
}
