package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.order.entity.OrderSub;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderSubMapper extends BaseMapper<OrderSub> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND deleted = 0
            ORDER BY id ASC
            """)
  List<OrderSub> listActiveByMainOrderId(@Param("mainOrderId") Long mainOrderId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT *
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE deleted = 0
              AND main_order_id IN
              <foreach collection="mainOrderIds" item="mainOrderId" open="(" separator="," close=")">
                #{mainOrderId}
              </foreach>
            ORDER BY main_order_id ASC, id ASC
            </script>
            """)
  List<OrderSub> listActiveByMainOrderIds(@Param("mainOrderIds") List<Long> mainOrderIds);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND sub_order_no = #{subOrderNo}
              AND deleted = 0
            LIMIT 1
            """)
  OrderSub selectActiveByMainOrderIdAndSubOrderNo(
      @Param("mainOrderId") Long mainOrderId, @Param("subOrderNo") String subOrderNo);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT COUNT(1)
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND merchant_id = #{merchantId}
              AND deleted = 0
            """)
  long countActiveByMainOrderIdAndMerchantId(
      @Param("mainOrderId") Long mainOrderId, @Param("merchantId") Long merchantId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_sub
            WHERE order_status = 'SHIPPED'
              AND shipped_at IS NOT NULL
              AND shipped_at &lt;= #{deadline}
              AND deleted = 0
            ORDER BY shipped_at ASC
            LIMIT #{limit}
            """)
  List<OrderSub> listAutoConfirmCandidates(
      @Param("deadline") LocalDateTime deadline, @Param("limit") Integer limit);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT id
            FROM order_sub
            WHERE deleted = 0
              AND created_at &lt; #{timeoutPoint}
              AND order_status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            ORDER BY created_at ASC
            LIMIT #{limit}
            </script>
            """)
  List<Long> listTimeoutSubOrderIds(
      @Param("statuses") List<String> statuses,
      @Param("timeoutPoint") LocalDateTime timeoutPoint,
      @Param("limit") Integer limit);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT COUNT(1)
            FROM order_sub FORCE INDEX (idx_order_sub_main_deleted)
            WHERE main_order_id = #{mainOrderId}
              AND deleted = 0
              AND order_status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            </script>
            """)
  long countActiveByMainOrderIdAndStatuses(
      @Param("mainOrderId") Long mainOrderId, @Param("statuses") List<String> statuses);

  @Update(
      """
            UPDATE order_sub
            SET shipping_company = #{company},
                tracking_number = #{trackingNumber},
                estimated_arrival = #{estimatedArrival},
                shipping_status = #{shippingStatus},
                shipped_at = COALESCE(shipped_at, #{shippedAt})
            WHERE id = #{subOrderId}
              AND deleted = 0
            """)
  int updateShippingInfo(
      @Param("subOrderId") Long subOrderId,
      @Param("company") String company,
      @Param("trackingNumber") String trackingNumber,
      @Param("estimatedArrival") LocalDate estimatedArrival,
      @Param("shippedAt") LocalDateTime shippedAt,
      @Param("shippingStatus") String shippingStatus);
}
