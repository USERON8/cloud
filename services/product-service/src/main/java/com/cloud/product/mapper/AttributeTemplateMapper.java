package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.AttributeTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttributeTemplateMapper extends BaseMapper<AttributeTemplate> {

  List<AttributeTemplate> selectByCategoryId(@Param("categoryId") Long categoryId);

  int incrementUsageCount(@Param("templateId") Long templateId);
}
