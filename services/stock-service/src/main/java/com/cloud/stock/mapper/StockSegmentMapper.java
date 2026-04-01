package com.cloud.stock.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.module.entity.StockSegment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockSegmentMapper extends BaseMapper<StockSegment> {

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT sku_id AS skuId,
                   SUM(available_qty) AS availableQty,
                   SUM(locked_qty) AS lockedQty,
                   SUM(sold_qty) AS soldQty,
                   COUNT(*) AS segmentCount,
                   MAX(alert_threshold) AS alertThreshold,
                   MAX(status) AS status,
                   MIN(created_at) AS createdAt,
                   MAX(updated_at) AS updatedAt
            FROM stock_segment
            WHERE sku_id = #{skuId}
              AND deleted = 0
              AND status = 1
            GROUP BY sku_id
            LIMIT 1
            """)
  StockLedgerVO selectLedgerBySkuId(@Param("skuId") Long skuId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            <script>
            SELECT sku_id AS skuId,
                   SUM(available_qty) AS availableQty,
                   SUM(locked_qty) AS lockedQty,
                   SUM(sold_qty) AS soldQty,
                   COUNT(*) AS segmentCount,
                   MAX(alert_threshold) AS alertThreshold,
                   MAX(status) AS status,
                   MIN(created_at) AS createdAt,
                   MAX(updated_at) AS updatedAt
            FROM stock_segment
            WHERE deleted = 0
              AND status = 1
              AND sku_id IN
              <foreach collection="skuIds" item="skuId" open="(" separator="," close=")">
                #{skuId}
              </foreach>
            GROUP BY sku_id
            ORDER BY sku_id ASC
            </script>
            """)
  List<StockLedgerVO> listLedgersBySkuIds(@Param("skuIds") List<Long> skuIds);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT *
            FROM stock_segment
            WHERE sku_id = #{skuId}
              AND deleted = 0
              AND status = 1
            ORDER BY segment_id ASC
            """)
  List<StockSegment> listActiveSegmentsBySkuId(@Param("skuId") Long skuId);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT sku_id AS skuId,
                   SUM(available_qty) AS availableQty,
                   SUM(locked_qty) AS lockedQty,
                   SUM(sold_qty) AS soldQty,
                   COUNT(*) AS segmentCount,
                   MAX(alert_threshold) AS alertThreshold,
                   MAX(status) AS status,
                   MIN(created_at) AS createdAt,
                   MAX(updated_at) AS updatedAt
            FROM stock_segment
            WHERE deleted = 0
              AND status = 1
            GROUP BY sku_id
            HAVING SUM(available_qty) <= MAX(alert_threshold)
            ORDER BY sku_id ASC
            """)
  IPage<StockLedgerVO> pageLowStockLedgers(IPage<StockLedgerVO> page);

  @InterceptorIgnore(illegalSql = "1")
  @Select(
      """
            SELECT sku_id AS skuId,
                   SUM(available_qty) AS availableQty,
                   SUM(locked_qty) AS lockedQty,
                   SUM(sold_qty) AS soldQty,
                   COUNT(*) AS segmentCount,
                   MAX(alert_threshold) AS alertThreshold,
                   MAX(status) AS status,
                   MIN(created_at) AS createdAt,
                   MAX(updated_at) AS updatedAt
            FROM stock_segment
            WHERE deleted = 0
              AND status = 1
            GROUP BY sku_id
            ORDER BY sku_id ASC
            """)
  IPage<StockLedgerVO> pageActiveLedgers(IPage<StockLedgerVO> page);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE stock_segment
            SET available_qty = available_qty - #{qty},
                locked_qty = locked_qty + #{qty},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND segment_id = #{segmentId}
              AND deleted = 0
              AND status = 1
              AND available_qty >= #{qty}
            """)
  int reserveOnSegment(
      @Param("skuId") Long skuId, @Param("segmentId") Integer segmentId, @Param("qty") Integer qty);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE stock_segment
            SET locked_qty = locked_qty - #{qty},
                available_qty = available_qty + #{qty},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND segment_id = #{segmentId}
              AND deleted = 0
              AND status = 1
              AND locked_qty >= #{qty}
            """)
  int releaseOnSegment(
      @Param("skuId") Long skuId, @Param("segmentId") Integer segmentId, @Param("qty") Integer qty);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE stock_segment
            SET locked_qty = locked_qty - #{qty},
                sold_qty = sold_qty + #{qty},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND segment_id = #{segmentId}
              AND deleted = 0
              AND status = 1
              AND locked_qty >= #{qty}
            """)
  int confirmLockedOnSegment(
      @Param("skuId") Long skuId, @Param("segmentId") Integer segmentId, @Param("qty") Integer qty);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE stock_segment
            SET sold_qty = sold_qty - #{qty},
                available_qty = available_qty + #{qty},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND segment_id = #{segmentId}
              AND deleted = 0
              AND status = 1
              AND sold_qty >= #{qty}
            """)
  int restoreSoldOnSegment(
      @Param("skuId") Long skuId, @Param("segmentId") Integer segmentId, @Param("qty") Integer qty);

  @InterceptorIgnore(illegalSql = "1")
  @Update(
      """
            UPDATE stock_segment
            SET available_qty = available_qty - #{qty},
                sold_qty = sold_qty + #{qty},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND segment_id = #{segmentId}
              AND deleted = 0
              AND status = 1
              AND available_qty >= #{qty}
            """)
  int sellDirectlyOnSegment(
      @Param("skuId") Long skuId, @Param("segmentId") Integer segmentId, @Param("qty") Integer qty);
}
