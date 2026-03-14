package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.stock.module.entity.StockReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            <script>
            SELECT *
            FROM stock_reservation FORCE INDEX (idx_stock_reservation_sub_sku_deleted)
            WHERE deleted = 0
            <if test='subOrderNos != null and subOrderNos.size() > 0'>
              AND sub_order_no IN
              <foreach collection='subOrderNos' item='subOrderNo' open='(' separator=',' close=')'>
                #{subOrderNo}
              </foreach>
            </if>
            <if test='skuIds != null and skuIds.size() > 0'>
              AND sku_id IN
              <foreach collection='skuIds' item='skuId' open='(' separator=',' close=')'>
                #{skuId}
              </foreach>
            </if>
            </script>
            """)
    List<StockReservation> selectActiveBySubOrderNosAndSkuIds(@Param("subOrderNos") List<String> subOrderNos,
                                                              @Param("skuIds") List<Long> skuIds);

    @InterceptorIgnore(illegalSql = "1")
    @Update("""
            <script>
            UPDATE stock_reservation
            SET status = 'ROLLED_BACK', updated_at = NOW()
            WHERE deleted = 0
              AND id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </script>
            """)
    int markRolledBackByIds(@Param("ids") List<Long> ids);
}
