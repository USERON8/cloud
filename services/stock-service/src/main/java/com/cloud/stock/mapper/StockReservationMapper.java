package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockReservationMapper extends BaseMapper<StockReservation> {

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM stock_reservation FORCE INDEX (idx_stock_reservation_sub_sku_deleted)
            WHERE sub_order_no = #{subOrderNo}
              AND sku_id = #{skuId}
              AND deleted = 0
            LIMIT 1
            """)
    StockReservation selectActiveBySubOrderNoAndSkuId(@Param("subOrderNo") String subOrderNo,
                                                      @Param("skuId") Long skuId);
}
