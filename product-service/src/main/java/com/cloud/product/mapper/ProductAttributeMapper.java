package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性 Mapper
 *
 * @author what's up
 */
@Mapper
public interface ProductAttributeMapper extends BaseMapper<ProductAttribute> {
    /**
     * 根据商品ID查询属性列表
     *
     * @param productId 商品ID
     * @return 属性列表
     */
    List<ProductAttribute> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据商品ID和属性分组查询属性
     *
     * @param productId 商品ID
     * @param attrGroup 属性分组
     * @return 属性列表
     */
    List<ProductAttribute> selectByProductIdAndGroup(@Param("productId") Long productId,
                                                     @Param("attrGroup") String attrGroup);

    /**
     * 批量插入属性
     *
     * @param attributes 属性列表
     * @return 插入数量
     */
    int batchInsert(@Param("attributes") List<ProductAttribute> attributes);

    /**
     * 根据商品ID删除属性
     *
     * @param productId 商品ID
     * @return 删除数量
     */
    int deleteByProductId(@Param("productId") Long productId);
}
