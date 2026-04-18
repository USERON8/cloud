package com.cloud.common.domain.vo.governance;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class ThreadPoolMetricsVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private String name;
  private Integer corePoolSize;
  private Integer maxPoolSize;
  private Integer activeCount;
  private Integer poolSize;
  private Integer queueSize;
  private Long completedTaskCount;
  private Long taskCount;
  private Integer queueRemainingCapacity;
}
