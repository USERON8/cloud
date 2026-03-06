package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;






@Mapper
public interface BrandMapper extends BaseMapper<Brand> {
    





    List<Brand> selectHotBrands(@Param("limit") Integer limit);

    





    List<Brand> selectRecommendedBrands(@Param("limit") Integer limit);

    






    int incrementProductCount(@Param("brandId") Long brandId, @Param("count") Integer count);

    






    int decrementProductCount(@Param("brandId") Long brandId, @Param("count") Integer count);
}
