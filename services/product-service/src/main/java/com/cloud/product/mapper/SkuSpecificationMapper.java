package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.SkuSpecification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkuSpecificationMapper extends BaseMapper<SkuSpecification> {

  List<SkuSpecification> selectByCategoryId(@Param("categoryId") Long categoryId);

  List<SkuSpecification> selectCommonSpecs();
}
