package com.cloud.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.order.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM order_item FORCE INDEX (idx_order_item_sub_deleted)
            WHERE sub_order_id = #{subOrderId}
              AND deleted = 0
            ORDER BY id ASC
            """)
  List<OrderItem> listActiveBySubOrderId(@Param("subOrderId") Long subOrderId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT *
            FROM order_item FORCE INDEX (idx_order_item_sub_deleted)
            WHERE deleted = 0
              AND sub_order_id IN
              <foreach collection="subOrderIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY sub_order_id ASC, id ASC
            </script>
            """)
  List<OrderItem> listActiveBySubOrderIds(@Param("subOrderIds") List<Long> subOrderIds);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT oi.spu_id AS productId,
                   SUM(oi.quantity) AS sellCount
            FROM order_item oi
            JOIN order_sub os ON oi.sub_order_id = os.id
            WHERE oi.deleted = 0
              AND os.deleted = 0
              AND os.order_status = 'DONE'
              AND os.done_at &gt;= #{start}
              AND os.done_at &lt; #{end}
            GROUP BY oi.spu_id
            ORDER BY sellCount DESC
            LIMIT #{limit}
            """)
  List<ProductSellStatDTO> listDailySellStats(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") Integer limit);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT oi.spu_id AS productId,
                   SUM(oi.quantity) AS sellCount
            FROM order_item oi
            JOIN order_sub os ON oi.sub_order_id = os.id
            WHERE oi.deleted = 0
              AND os.deleted = 0
              AND os.order_status = 'DONE'
              AND oi.spu_id IN
              <foreach collection="productIds" item="productId" open="(" separator="," close=")">
                #{productId}
              </foreach>
            GROUP BY oi.spu_id
            </script>
            """)
  List<ProductSellStatDTO> listSellStatsByProductIds(@Param("productIds") List<Long> productIds);
}
