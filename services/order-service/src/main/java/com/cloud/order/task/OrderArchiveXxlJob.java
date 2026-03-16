package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderArchiveXxlJob {

  private final JdbcTemplate jdbcTemplate;

  @Value("${order.archive.enabled:true}")
  private boolean enabled;

  @Value("${order.archive.after-days:180}")
  private int archiveAfterDays;

  @Value("${order.archive.batch-size:200}")
  private int batchSize;

  @XxlJob("orderArchiveJob")
  @DistributedLock(
      key = "'xxl:order:archive'",
      waitTime = 1,
      leaseTime = 1800,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  @Transactional(rollbackFor = Exception.class)
  public void archiveOrders() {
    if (!enabled) {
      XxlJobHelper.log("orderArchiveJob skipped, disabled");
      return;
    }
    int safeBatchSize = batchSize <= 0 ? 200 : batchSize;
    int safeAfterDays = archiveAfterDays <= 0 ? 180 : archiveAfterDays;
    LocalDateTime cutoff = LocalDateTime.now().minusDays(safeAfterDays);

    List<Long> mainIds =
        jdbcTemplate.queryForList(
            """
                SELECT id
                FROM order_main
                WHERE deleted = 0
                  AND order_status IN ('DONE', 'CANCELLED', 'CLOSED')
                  AND updated_at < ?
                ORDER BY updated_at ASC
                LIMIT ?
                """,
            Long.class,
            Timestamp.valueOf(cutoff),
            safeBatchSize);

    if (mainIds == null || mainIds.isEmpty()) {
      XxlJobHelper.log("orderArchiveJob finished, empty batch");
      return;
    }

    String idClause = mainIds.stream().map(String::valueOf).collect(Collectors.joining(","));

    int archivedMain =
        jdbcTemplate.update(
            """
                INSERT IGNORE INTO order_main_archive (
                    id, main_order_no, user_id, order_status, total_amount, payable_amount, pay_channel,
                    paid_at, cancelled_at, cancel_reason, remark, idempotency_key, created_at, updated_at,
                    deleted, version, archived_at
                )
                SELECT id, main_order_no, user_id, order_status, total_amount, payable_amount, pay_channel,
                       paid_at, cancelled_at, cancel_reason, remark, idempotency_key, created_at, updated_at,
                       deleted, version, NOW()
                FROM order_main
                WHERE id IN ("""
                + idClause
                + ")");

    int archivedSub =
        jdbcTemplate.update(
            """
                INSERT IGNORE INTO order_sub_archive (
                    id, sub_order_no, main_order_id, merchant_id, order_status, shipping_status, after_sale_status,
                    item_amount, shipping_fee, discount_amount, payable_amount, receiver_name, receiver_phone,
                    receiver_address, shipping_company, tracking_number, shipped_at, estimated_arrival, received_at,
                    done_at, closed_at, close_reason, created_at, updated_at, deleted, version, archived_at
                )
                SELECT id, sub_order_no, main_order_id, merchant_id, order_status, shipping_status, after_sale_status,
                       item_amount, shipping_fee, discount_amount, payable_amount, receiver_name, receiver_phone,
                       receiver_address, shipping_company, tracking_number, shipped_at, estimated_arrival, received_at,
                       done_at, closed_at, close_reason, created_at, updated_at, deleted, version, NOW()
                FROM order_sub
                WHERE main_order_id IN ("""
                + idClause
                + ")");

    int archivedItems =
        jdbcTemplate.update(
            """
                INSERT IGNORE INTO order_item_archive (
                    id, main_order_id, sub_order_id, spu_id, sku_id, sku_code, sku_name, sku_snapshot,
                    quantity, unit_price, total_price, created_at, updated_at, deleted, version, archived_at
                )
                SELECT id, main_order_id, sub_order_id, spu_id, sku_id, sku_code, sku_name, sku_snapshot,
                       quantity, unit_price, total_price, created_at, updated_at, deleted, version, NOW()
                FROM order_item
                WHERE main_order_id IN ("""
                + idClause
                + ")");

    int deletedItems =
        jdbcTemplate.update("DELETE FROM order_item WHERE main_order_id IN (" + idClause + ")");
    int deletedSubs =
        jdbcTemplate.update("DELETE FROM order_sub WHERE main_order_id IN (" + idClause + ")");
    int deletedMains = jdbcTemplate.update("DELETE FROM order_main WHERE id IN (" + idClause + ")");

    String message =
        String.format(
            "orderArchiveJob finished, main=%d sub=%d item=%d deletedMain=%d deletedSub=%d deletedItem=%d",
            archivedMain, archivedSub, archivedItems, deletedMains, deletedSubs, deletedItems);
    XxlJobHelper.log(message);
    log.info(message);
  }
}
