package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM order_item FORCE INDEX (idx_order_item_sub_deleted)
            WHERE sub_order_id = #{subOrderId}
              AND deleted = 0
            ORDER BY id ASC
            """)
    List<OrderItem> listActiveBySubOrderId(@Param("subOrderId") Long subOrderId);
}
