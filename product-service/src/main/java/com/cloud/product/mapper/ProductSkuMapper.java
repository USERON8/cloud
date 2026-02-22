package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;






@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    





    List<ProductSku> selectByProductId(@Param("productId") Long productId);

    





    ProductSku selectBySkuCode(@Param("skuCode") String skuCode);

    






    int batchUpdateStatus(@Param("skuIds") List<Long> skuIds, @Param("status") Integer status);
}
