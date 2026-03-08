package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.OrderMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMainMapper extends BaseMapper<OrderMain> {

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM order_main FORCE INDEX (idx_order_main_idempotency_deleted)
            WHERE idempotency_key = #{idempotencyKey}
              AND deleted = 0
            LIMIT 1
            """)
    OrderMain selectActiveByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
