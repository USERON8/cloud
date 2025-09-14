package com.cloud.common.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建DTO
 * 用于创建订单时的参数传输
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
@Schema(description = "订单创建DTO")
public class OrderCreateDTO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 店铺ID
     */
    @Schema(description = "店铺ID")
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;
    
    /**
     * 收货人姓名
     */
    @Schema(description = "收货人姓名")
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;
    
    /**
     * 收货人电话
     */
    @Schema(description = "收货人电话")
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;
    
    /**
     * 收货人地址
     */
    @Schema(description = "收货人地址")
    @NotBlank(message = "收货人地址不能为空")
    private String receiverAddress;
    
    /**
     * 支付方式：1-微信支付，2-支付宝，3-银联支付
     */
    @Schema(description = "支付方式：1-微信支付，2-支付宝，3-银联支付")
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
    
    /**
     * 订单总金额
     */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;
    
    /**
     * 实付金额
     */
    @Schema(description = "实付金额")
    private BigDecimal payAmount;
    
    /**
     * 收货地址ID
     */
    @Schema(description = "收货地址ID")
    private Long addressId;
    
    /**
     * 优惠金额
     */
    @Schema(description = "优惠金额")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 运费
     */
    @Schema(description = "运费")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    /**
     * 订单备注
     */
    @Schema(description = "订单备注")
    private String remark;
    
    /**
     * 订单项列表
     */
    @Schema(description = "订单项列表")
    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemDTO> orderItems;
    
    /**
     * 订单创建项DTO
     */
    @Data
    @Schema(description = "订单创建项DTO")
    public static class OrderItemDTO implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 商品ID
         */
        @Schema(description = "商品ID")
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        
        /**
         * 商品名称
         */
        @Schema(description = "商品名称")
        @NotBlank(message = "商品名称不能为空")
        private String productName;
        
        /**
         * 商品图片
         */
        @Schema(description = "商品图片")
        private String productImage;
        
        /**
         * 商品价格（当前价格）
         */
        @Schema(description = "商品价格（当前价格）")
        @NotNull(message = "商品价格不能为空")
        @Positive(message = "商品价格必须大于0")
        private BigDecimal price;
        
        /**
         * 商品价格（与productPrice同义，为了兼容）
         */
        @Schema(description = "商品价格")
        private BigDecimal productPrice;
        
        /**
         * 购买数量
         */
        @Schema(description = "购买数量")
        @NotNull(message = "购买数量不能为空")
        @Positive(message = "购买数量必须大于0")
        private Integer quantity;
        
        /**
         * 商品快照（JSON字符串）
         */
        @Schema(description = "商品快照（JSON字符串）")
        private Object productSnapshot;
        
        /**
         * 商品规格（JSON字符串）
         */
        @Schema(description = "商品规格（JSON字符串）")
        private String productSpecs;
    }
}
