package com.cloud.common.domain.vo.order;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderSubStatusVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long mainOrderId;
  private Long subOrderId;
  private String mainOrderNo;
  private String subOrderNo;
  private String orderStatus;
  private Long userId;
  private BigDecimal payableAmount;
}
