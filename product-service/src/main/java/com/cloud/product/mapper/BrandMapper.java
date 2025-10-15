package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 品牌 Mapper
 *
 * @author what's up
 */
@Mapper
public interface BrandMapper extends BaseMapper<Brand> {
    /**
     * 查询热门品牌
     *
     * @param limit 数量限制
     * @return 热门品牌列表
     */
    List<Brand> selectHotBrands(@Param("limit") Integer limit);

    /**
     * 查询推荐品牌
     *
     * @param limit 数量限制
     * @return 推荐品牌列表
     */
    List<Brand> selectRecommendedBrands(@Param("limit") Integer limit);

    /**
     * 增加品牌关联商品数量
     *
     * @param brandId 品牌ID
     * @param count   增加数量
     * @return 更新数量
     */
    int incrementProductCount(@Param("brandId") Long brandId, @Param("count") Integer count);

    /**
     * 减少品牌关联商品数量
     *
     * @param brandId 品牌ID
     * @param count   减少数量
     * @return 更新数量
     */
    int decrementProductCount(@Param("brandId") Long brandId, @Param("count") Integer count);
}
