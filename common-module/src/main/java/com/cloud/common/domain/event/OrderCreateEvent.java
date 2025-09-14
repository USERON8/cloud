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
 * 订单创建事件对象
 * 用于在服务间传递订单创建信息，库存服务接收到此消息后需要冻结相应商品的库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单项列表
     */
    private List<OrderItem> orderItems;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 订单项信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品数量
         */
        private Integer quantity;

        /**
         * 购买时单价
         */
        private BigDecimal price;
    }
}