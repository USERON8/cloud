package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.order.entity.OrderMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            SELECT *
            FROM order_main FORCE INDEX (uk_order_main_no)
            WHERE main_order_no = #{mainOrderNo}
              AND deleted = 0
            LIMIT 1
            """)
    OrderMain selectActiveByOrderNo(@Param("mainOrderNo") String mainOrderNo);

    @InterceptorIgnore(illegalSql = "1")
    @Select("""
            <script>
            SELECT DISTINCT om.*
            FROM order_main om
            JOIN order_sub os ON os.main_order_id = om.id AND os.deleted = 0
            WHERE om.deleted = 0
              AND os.merchant_id = #{merchantId}
              <if test="statuses != null and statuses.size() > 0">
                AND om.order_status IN
                <foreach collection="statuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="userId != null">
                AND om.user_id = #{userId}
              </if>
            ORDER BY om.id DESC
            </script>
            """)
    IPage<OrderMain> selectPageByMerchant(IPage<OrderMain> page,
                                          @Param("merchantId") Long merchantId,
                                          @Param("statuses") List<String> statuses,
                                          @Param("userId") Long userId);
}
