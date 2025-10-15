package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品SKU Mapper
 *
 * @author what's up
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    /**
     * 根据商品ID查询SKU列表
     *
     * @param productId 商品ID
     * @return SKU列表
     */
    List<ProductSku> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据SKU编码查询SKU
     *
     * @param skuCode SKU编码
     * @return SKU信息
     */
    ProductSku selectBySkuCode(@Param("skuCode") String skuCode);

    /**
     * 批量更新SKU状态
     *
     * @param skuIds SKU ID列表
     * @param status 状态
     * @return 更新数量
     */
    int batchUpdateStatus(@Param("skuIds") List<Long> skuIds, @Param("status") Integer status);
}
