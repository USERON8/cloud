package com.cloud.common.task;

import com.cloud.common.annotation.DistributedLock;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class SeataUndoLogCleanupXxlJob {

  private final JdbcTemplate jdbcTemplate;

  @Value("${seata.undo-log.retention-days:7}")
  private int retentionDays;

  @Value("${app.job.lock-prefix:}")
  private String lockPrefix;

  @Value("${spring.application.name:service}")
  private String serviceName;

  @XxlJob("seataUndoLogCleanJob")
  @DistributedLock(
      key = "#target.lockKey()",
      waitTime = 1,
      leaseTime = 600,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void cleanUndoLog() {
    int effectiveDays = retentionDays <= 0 ? 7 : retentionDays;
    LocalDateTime cutoff = LocalDateTime.now().minusDays(effectiveDays);
    int deleted =
        jdbcTemplate.update(
            "DELETE FROM undo_log WHERE log_created < ?", Timestamp.valueOf(cutoff));
    String message = "seataUndoLogCleanJob finished, deleted=" + deleted;
    XxlJobHelper.log(message);
    log.info(message);
  }

  public String lockKey() {
    String prefix = lockPrefix;
    if (prefix == null || prefix.isBlank()) {
      prefix = "xxl:" + serviceName;
    }
    return prefix + ":undo-log-clean";
  }
}
