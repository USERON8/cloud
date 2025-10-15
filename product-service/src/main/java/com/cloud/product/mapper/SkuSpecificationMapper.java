package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.SkuSpecification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU规格定义 Mapper
 *
 * @author what's up
 */
@Mapper
public interface SkuSpecificationMapper extends BaseMapper<SkuSpecification> {
    /**
     * 根据分类ID查询规格列表
     *
     * @param categoryId 分类ID
     * @return 规格列表
     */
    List<SkuSpecification> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 查询通用规格(categoryId=0)
     *
     * @return 通用规格列表
     */
    List<SkuSpecification> selectCommonSpecs();
}
