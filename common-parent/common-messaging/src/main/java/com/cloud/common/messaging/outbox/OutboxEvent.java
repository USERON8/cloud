package com.cloud.common.messaging.outbox;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outbox_event")
public class OutboxEvent extends BaseEntity<OutboxEvent> {

  @TableField("event_id")
  private String eventId;

  @TableField("aggregate_type")
  private String aggregateType;

  @TableField("aggregate_id")
  private String aggregateId;

  @TableField("event_type")
  private String eventType;

  @TableField("payload")
  private String payload;

  @TableField("status")
  private String status;

  @TableField("retry_count")
  private Integer retryCount;

  @TableField("next_retry_at")
  private LocalDateTime nextRetryAt;
}
