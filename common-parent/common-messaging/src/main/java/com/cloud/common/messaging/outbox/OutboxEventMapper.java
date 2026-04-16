package com.cloud.common.messaging.outbox;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

  @Select(
      "SELECT id, event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_retry_at, "
          + "created_at, updated_at, deleted, version "
          + "FROM outbox_event "
          + "WHERE deleted = 0 "
          + "AND status IN ('NEW','FAILED') "
          + "AND (next_retry_at IS NULL OR next_retry_at <= NOW()) "
          + "ORDER BY created_at ASC "
          + "LIMIT #{limit}")
  @InterceptorIgnore(illegalSql = "1")
  List<OutboxEvent> selectDueEvents(@Param("limit") int limit);

  @Update(
      "UPDATE outbox_event SET status = 'PROCESSING', updated_at = NOW() "
          + "WHERE id = #{id} AND status IN ('NEW','FAILED') AND deleted = 0")
  int markProcessing(@Param("id") Long id);

  @Update(
      "UPDATE outbox_event SET status = 'SENT', updated_at = NOW() "
          + "WHERE id = #{id} AND deleted = 0")
  int markSent(@Param("id") Long id);

  @Update(
      "UPDATE outbox_event SET status = #{status}, retry_count = #{retryCount}, next_retry_at = #{nextRetryAt}, "
          + "updated_at = NOW() WHERE id = #{id} AND deleted = 0")
  int updateStatus(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("retryCount") int retryCount,
      @Param("nextRetryAt") LocalDateTime nextRetryAt);

  @Select(
      "SELECT COUNT(1) FROM outbox_event WHERE deleted = 0 AND status IN ('NEW','FAILED','PROCESSING')")
  @InterceptorIgnore(illegalSql = "1")
  long countPending();

  @Select(
      "SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), NOW()), 0) "
          + "FROM outbox_event WHERE deleted = 0 AND status IN ('NEW','FAILED','PROCESSING')")
  @InterceptorIgnore(illegalSql = "1")
  long oldestPendingAgeSeconds();

  @Select("SELECT COUNT(1) FROM outbox_event WHERE deleted = 0 AND status = #{status}")
  @InterceptorIgnore(illegalSql = "1")
  long countByStatus(@Param("status") String status);

  @Select({
    "<script>",
    "SELECT id, event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_retry_at, ",
    "created_at, updated_at, deleted, version ",
    "FROM outbox_event ",
    "WHERE deleted = 0 AND status IN ",
    "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
    "#{status}",
    "</foreach>",
    "ORDER BY created_at ASC ",
    "LIMIT #{limit}",
    "</script>"
  })
  @InterceptorIgnore(illegalSql = "1")
  List<OutboxEvent> selectByStatuses(
      @Param("statuses") List<String> statuses, @Param("limit") int limit);

  @Update(
      "UPDATE outbox_event SET status = 'NEW', retry_count = 0, next_retry_at = NOW(), updated_at = NOW() "
          + "WHERE id = #{id} AND deleted = 0 AND status IN ('FAILED','DEAD')")
  int requeue(@Param("id") Long id);
}
