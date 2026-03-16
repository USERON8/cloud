package com.cloud.common.domain;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class PageQuery implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long current = 1L;

  private Long size = 10L;

  private String orderBy;

  private String orderDirection;
}
