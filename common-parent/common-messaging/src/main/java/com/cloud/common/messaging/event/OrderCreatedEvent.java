package com.cloud.common.messaging.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long mainOrderId;

  private String orderNo;

  private Long userId;

  private BigDecimal totalAmount;

  private Integer timeoutMinutes;

  private List<SubOrderStock> subOrders;

  private String remark;

  private Long timestamp;

  private String eventId;

  private String eventType;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SubOrderStock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long subOrderId;

    private String subOrderNo;

    private List<SkuQuantity> items;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SkuQuantity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private Integer quantity;
  }
}
