package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;






@Mapper
public interface ProductAttributeMapper extends BaseMapper<ProductAttribute> {
    





    List<ProductAttribute> selectByProductId(@Param("productId") Long productId);

    






    List<ProductAttribute> selectByProductIdAndGroup(@Param("productId") Long productId,
                                                     @Param("attrGroup") String attrGroup);

    





    int batchInsert(@Param("attributes") List<ProductAttribute> attributes);

    





    int deleteByProductId(@Param("productId") Long productId);
}
