package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.AttributeTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;






@Mapper
public interface AttributeTemplateMapper extends BaseMapper<AttributeTemplate> {
    





    List<AttributeTemplate> selectByCategoryId(@Param("categoryId") Long categoryId);

    





    int incrementUsageCount(@Param("templateId") Long templateId);
}
