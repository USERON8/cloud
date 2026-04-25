package com.cloud.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OrderSummaryDTO {
  private Long id;
  private String orderNo;
  private Long userId;
  private List<SubOrderSummaryDTO> subOrders;
  private Long subOrderId;
  private String subOrderNo;
  private Long merchantId;
  private Long afterSaleId;
  private String afterSaleNo;
  private String afterSaleType;
  private String refundNo;
  private BigDecimal totalAmount;
  private BigDecimal payAmount;
  private Integer status;
  private String orderStatusRaw;
  private String afterSaleStatus;
  private LocalDateTime createdAt;
  private List<OrderItemSummaryDTO> items;

  @Data
  public static class SubOrderSummaryDTO {
    private Long subOrderId;
    private String subOrderNo;
    private Long merchantId;
    private Long afterSaleId;
    private String afterSaleNo;
    private String afterSaleType;
    private String refundNo;
    private BigDecimal payAmount;
    private Integer status;
    private String orderStatusRaw;
    private String afterSaleStatus;
  }

  @Data
  public static class OrderItemSummaryDTO {
    private Long id;
    private Long subOrderId;
    private Long spuId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Map<String, Object> skuSnapshot;
    private LatestProductDTO latestProduct;
  }

  @Data
  public static class LatestProductDTO {
    private Long spuId;
    private Long skuId;
    private String spuName;
    private String skuCode;
    private String skuName;
    private String specJson;
    private BigDecimal salePrice;
    private BigDecimal marketPrice;
    private String imageUrl;
    private String imageFile;
    private Integer status;
    private String brandName;
    private String categoryName;
    private Long merchantId;
    private String shopName;
  }
}
