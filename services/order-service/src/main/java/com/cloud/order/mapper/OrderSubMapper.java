package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.OrderSub;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
