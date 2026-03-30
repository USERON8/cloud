package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockReservation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockReservationMapper extends BaseMapper<StockReservation> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM stock_reservation
            WHERE sub_order_no = #{subOrderNo}
              AND sku_id = #{skuId}
              AND deleted = 0
            ORDER BY segment_id ASC, id ASC
            """)
  List<StockReservation> listActiveBySubOrderNoAndSkuId(
      @Param("subOrderNo") String subOrderNo, @Param("skuId") Long skuId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM stock_reservation
            WHERE sub_order_no = #{subOrderNo}
              AND deleted = 0
            ORDER BY sku_id ASC, segment_id ASC, id ASC
            """)
  List<StockReservation> listActiveBySubOrderNo(@Param("subOrderNo") String subOrderNo);
}
