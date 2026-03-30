package com.cloud.common.messaging.deadletter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@Slf4j
public class JdbcDeadLetterOpsService implements DeadLetterOpsService {

  private static final String COUNT_PENDING_SQL =
      "SELECT COUNT(1) FROM dead_letter WHERE status = 0";

  private static final String LIST_PENDING_SQL =
      "SELECT id, topic, msg_id, payload, fail_reason, error_msg, status, service, created_at, handled_at "
          + "FROM dead_letter WHERE status = 0 ORDER BY created_at DESC LIMIT ?";

  private static final String MARK_HANDLED_SQL =
      "UPDATE dead_letter SET status = 1, handled_at = NOW() WHERE topic = ? AND msg_id = ? AND status = 0";

  private final JdbcTemplate jdbcTemplate;

  public JdbcDeadLetterOpsService(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public long countPending() {
    try {
      Long count = jdbcTemplate.queryForObject(COUNT_PENDING_SQL, Long.class);
      return count == null ? 0L : count;
    } catch (Exception ex) {
      log.warn("Count pending dead letters failed", ex);
      return 0L;
    }
  }

  @Override
  public List<DeadLetterRecord> listPending(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 500));
    try {
      return jdbcTemplate.query(LIST_PENDING_SQL, new DeadLetterRowMapper(), safeLimit);
    } catch (Exception ex) {
      log.warn("List pending dead letters failed: limit={}", safeLimit, ex);
      return List.of();
    }
  }

  @Override
  public boolean markHandled(String topic, String msgId) {
    try {
      return jdbcTemplate.update(MARK_HANDLED_SQL, safe(topic), safe(msgId)) > 0;
    } catch (Exception ex) {
      log.warn("Mark dead letter handled failed: topic={}, msgId={}", topic, msgId, ex);
      return false;
    }
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private static final class DeadLetterRowMapper implements RowMapper<DeadLetterRecord> {

    @Override
    public DeadLetterRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      return DeadLetterRecord.builder()
          .id(rs.getLong("id"))
          .topic(rs.getString("topic"))
          .msgId(rs.getString("msg_id"))
          .payload(rs.getString("payload"))
          .failReason(rs.getString("fail_reason"))
          .errorMsg(rs.getString("error_msg"))
          .status(rs.getInt("status"))
          .service(rs.getString("service"))
          .createdAt(
              rs.getTimestamp("created_at") == null
                  ? null
                  : rs.getTimestamp("created_at").toLocalDateTime())
          .handledAt(
              rs.getTimestamp("handled_at") == null
                  ? null
                  : rs.getTimestamp("handled_at").toLocalDateTime())
          .build();
    }
  }
}
