package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.OrderSub;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderSubMapper extends BaseMapper<OrderSub> {

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND deleted = 0
            ORDER BY id ASC
            """)
    List<OrderSub> listActiveByMainOrderId(@Param("mainOrderId") Long mainOrderId);

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND sub_order_no = #{subOrderNo}
              AND deleted = 0
            LIMIT 1
            """)
    OrderSub selectActiveByMainOrderIdAndSubOrderNo(@Param("mainOrderId") Long mainOrderId,
                                                    @Param("subOrderNo") String subOrderNo);

    @Update("""
            UPDATE order_sub
            SET shipping_company = #{company},
                tracking_number = #{trackingNumber},
                estimated_arrival = #{estimatedArrival},
                shipping_status = #{shippingStatus},
                shipped_at = COALESCE(shipped_at, #{shippedAt})
            WHERE id = #{subOrderId}
              AND deleted = 0
            """)
    int updateShippingInfo(@Param("subOrderId") Long subOrderId,
                           @Param("company") String company,
                           @Param("trackingNumber") String trackingNumber,
                           @Param("estimatedArrival") LocalDate estimatedArrival,
                           @Param("shippedAt") LocalDateTime shippedAt,
                           @Param("shippingStatus") String shippingStatus);
}
