package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.AttributeTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性模板 Mapper
 *
 * @author what's up
 */
@Mapper
public interface AttributeTemplateMapper extends BaseMapper<AttributeTemplate> {
    /**
     * 根据分类ID查询模板列表
     *
     * @param categoryId 分类ID
     * @return 模板列表
     */
    List<AttributeTemplate> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 增加模板使用次数
     *
     * @param templateId 模板ID
     * @return 更新数量
     */
    int incrementUsageCount(@Param("templateId") Long templateId);
}
