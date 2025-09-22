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
 * 订单创建事件
 * 用于订单创建时通知支付服务和库存服务
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
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
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 支付方式：1-支付宝，2-微信，3-银行卡
     */
    private Integer paymentMethod;

    /**
     * 收货地址ID
     */
    private Long addressId;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 收货地址详情
     */
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 订单商品列表
     */
    private List<OrderItemInfo> orderItems;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 订单商品信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo implements Serializable {
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
         * 商品图片
         */
        private String productImage;

        /**
         * 商品单价
         */
        private BigDecimal unitPrice;

        /**
         * 购买数量
         */
        private Integer quantity;

        /**
         * 小计金额
         */
        private BigDecimal subtotal;

        /**
         * 商家ID
         */
        private Long merchantId;

        /**
         * 商家名称
         */
        private String merchantName;
    }
}
