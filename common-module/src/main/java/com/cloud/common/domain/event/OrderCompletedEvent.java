package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单完成事件
 * 用于订单完成后通知库存服务解冻并扣减库存
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态：1-待支付，2-待发货，3-待收货，4-已完成，5-已取消
     */
    private Integer orderStatus;

    /**
     * 订单前状态
     */
    private Integer beforeStatus;

    /**
     * 订单后状态
     */
    private Integer afterStatus;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 发货时间
     */
    private LocalDateTime shippedTime;

    /**
     * 收货时间
     */
    private LocalDateTime receivedTime;

    /**
     * 物流公司
     */
    private String logisticsCompany;

    /**
     * 物流单号
     */
    private String trackingNumber;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 订单商品列表（用于库存扣减）
     */
    private List<StockDeductionInfo> stockDeductions;

    /**
     * 完成原因
     */
    private String completionReason;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 库存扣减信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockDeductionInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 商品SKU
         */
        private String productSku;

        /**
         * 需要扣减的数量
         */
        private Integer quantity;

        /**
         * 商家ID
         */
        private Long merchantId;

        /**
         * 商家名称
         */
        private String merchantName;

        /**
         * 仓库ID（如果有多仓库）
         */
        private Long warehouseId;

        /**
         * 仓库名称
         */
        private String warehouseName;

        /**
         * 库存操作类型：1-解冻，2-扣减，3-解冻并扣减
         */
        private Integer operationType;

        /**
         * 原始冻结数量
         */
        private Integer frozenQuantity;

        /**
         * 扣减原因
         */
        private String deductionReason;
    }
}
