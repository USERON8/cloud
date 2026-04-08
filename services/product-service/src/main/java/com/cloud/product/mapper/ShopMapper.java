package com.cloud.product.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.Shop;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ShopMapper extends BaseMapper<Shop> {

  @InterceptorIgnore(illegalSql = "1")
  @Select({
    "<script>",
    "SELECT *",
    "FROM merchant_shop FORCE INDEX (idx_shop_merchant_deleted)",
    "WHERE deleted = 0",
    "AND merchant_id IN",
    "<foreach collection='merchantIds' item='merchantId' open='(' separator=',' close=')'>",
    "#{merchantId}",
    "</foreach>",
    "</script>"
  })
  List<Shop> selectActiveByMerchantIds(@Param("merchantIds") List<Long> merchantIds);
}
