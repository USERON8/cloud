package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.SkuSpecification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;






@Mapper
public interface SkuSpecificationMapper extends BaseMapper<SkuSpecification> {
    





    List<SkuSpecification> selectByCategoryId(@Param("categoryId") Long categoryId);

    




    List<SkuSpecification> selectCommonSpecs();
}
