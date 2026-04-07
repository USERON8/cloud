package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.order.entity.OrderMain;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMainMapper extends BaseMapper<OrderMain> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_main FORCE INDEX (idx_order_main_idempotency_deleted)
            WHERE idempotency_key = #{idempotencyKey}
              AND deleted = 0
            LIMIT 1
            """)
  OrderMain selectActiveByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_main
            WHERE user_id = #{userId}
              AND client_order_id = #{clientOrderId}
              AND deleted = 0
            LIMIT 1
            """)
  OrderMain selectActiveByClientOrderId(
      @Param("userId") Long userId, @Param("clientOrderId") String clientOrderId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_main FORCE INDEX (uk_order_main_no)
            WHERE main_order_no = #{mainOrderNo}
              AND deleted = 0
            LIMIT 1
            """)
  OrderMain selectActiveByOrderNo(@Param("mainOrderNo") String mainOrderNo);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT om.*
            FROM order_main om
            FORCE INDEX (
              <choose>
                <when test="userId != null">idx_order_main_user_deleted_id</when>
                <otherwise>idx_order_main_deleted_id</otherwise>
              </choose>
            )
            WHERE om.deleted = 0
              <if test="userId != null">
                AND om.user_id = #{userId}
              </if>
            ORDER BY om.id DESC
            </script>
            """)
  IPage<OrderMain> selectPageActive(IPage<OrderMain> page, @Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
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
  IPage<OrderMain> selectPageByMerchant(
      IPage<OrderMain> page,
      @Param("merchantId") Long merchantId,
      @Param("statuses") List<String> statuses,
      @Param("userId") Long userId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT om.*
            FROM order_main om
            JOIN (
              SELECT os.main_order_id
              FROM order_main source_main
              JOIN order_sub os ON os.main_order_id = source_main.id AND os.deleted = 0
              WHERE source_main.deleted = 0
                <if test="merchantId != null">
                  AND os.merchant_id = #{merchantId}
                </if>
                <if test="userId != null">
                  AND source_main.user_id = #{userId}
                </if>
              GROUP BY os.main_order_id
              HAVING
                <choose>
                  <when test="statusCode == 0">
                    SUM(CASE WHEN os.order_status IN ('PAID', 'SHIPPED', 'DONE') THEN 1 ELSE 0 END) = 0
                    AND SUM(CASE WHEN os.order_status IN ('CREATED', 'STOCK_RESERVED') THEN 1 ELSE 0 END) &gt; 0
                  </when>
                  <when test="statusCode == 1">
                    SUM(CASE WHEN os.order_status IN ('CANCELLED', 'CLOSED') THEN 1 ELSE 0 END) &lt; COUNT(*)
                    AND SUM(CASE WHEN os.order_status = 'DONE' THEN 1 ELSE 0 END) &lt; COUNT(*)
                    AND SUM(CASE WHEN os.order_status = 'SHIPPED' THEN 1 ELSE 0 END) = 0
                    AND SUM(CASE WHEN os.order_status = 'PAID' THEN 1 ELSE 0 END) &gt; 0
                  </when>
                  <when test="statusCode == 2">
                    SUM(CASE WHEN os.order_status IN ('CANCELLED', 'CLOSED') THEN 1 ELSE 0 END) &lt; COUNT(*)
                    AND SUM(CASE WHEN os.order_status = 'DONE' THEN 1 ELSE 0 END) &lt; COUNT(*)
                    AND SUM(CASE WHEN os.order_status = 'SHIPPED' THEN 1 ELSE 0 END) &gt; 0
                  </when>
                  <when test="statusCode == 3">
                    SUM(CASE WHEN os.order_status = 'DONE' THEN 1 ELSE 0 END) = COUNT(*)
                  </when>
                  <when test="statusCode == 4">
                    SUM(CASE WHEN os.order_status IN ('CANCELLED', 'CLOSED') THEN 1 ELSE 0 END) = COUNT(*)
                  </when>
                </choose>
            ) filtered ON filtered.main_order_id = om.id
            WHERE om.deleted = 0
            ORDER BY om.id DESC
            </script>
            """)
  IPage<OrderMain> selectPageByVisibleStatus(
      IPage<OrderMain> page,
      @Param("merchantId") Long merchantId,
      @Param("userId") Long userId,
      @Param("statusCode") Integer statusCode);
}
