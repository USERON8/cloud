package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建请求DTO
 * 用于接收订单创建请求参数
 *
 * @author CloudDevAgent  
 * @since 2025-09-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单创建请求")
public class OrderCreateRequestDTO {
    
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "123456")
    private Long userId;
    
    @NotNull(message = "收货地址ID不能为空")
    @Schema(description = "收货地址ID", example = "789")
    private Long addressId;
    
    @DecimalMin(value = "0.01", message = "订单总金额必须大于0.01")
    @Schema(description = "订单总金额", example = "99.90")
    private BigDecimal totalAmount;
    
    @DecimalMin(value = "0.01", message = "实付金额必须大于0.01")  
    @Schema(description = "实付金额", example = "89.90")
    private BigDecimal payAmount;
    
    @NotEmpty(message = "订单商品列表不能为空")
    @Valid
    @Schema(description = "订单商品列表")
    private List<OrderItemCreateDTO> orderItems;
    
    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;
    
    /**
     * 订单商品创建DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单商品信息")
    public static class OrderItemCreateDTO {
        
        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID", example = "456789")
        private Long productId;
        
        @NotBlank(message = "商品名称不能为空")
        @Schema(description = "商品名称", example = "Apple iPhone 15")
        private String productName;
        
        @DecimalMin(value = "0.01", message = "商品单价必须大于0.01")
        @Schema(description = "商品单价", example = "6999.00")
        private BigDecimal price;
        
        @Min(value = 1, message = "商品数量必须大于0")
        @Schema(description = "商品数量", example = "2")
        private Integer quantity;
        
        @Schema(description = "商品规格信息", example = "256GB 深空黑色")
        private String specification;
        
        @Schema(description = "商品图片URL", example = "https://example.com/product.jpg")
        private String imageUrl;
    }
}
