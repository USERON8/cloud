package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM after_sale FORCE INDEX (idx_after_sale_sub_status_deleted)
            WHERE sub_order_id = #{subOrderId}
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """)
  AfterSale selectLatestActiveBySubOrderId(@Param("subOrderId") Long subOrderId);
}
