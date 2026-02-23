package com.cloud.common.domain.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Order DTO")
public class OrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "Order number")
    @Size(max = 64, message = "Order number length must be less than or equal to 64")
    private String orderNo;

    @Schema(description = "User ID")
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @Schema(description = "Order total amount")
    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    @Schema(description = "Actual payment amount")
    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal payAmount;

    @Schema(description = "Order status: 0-pending, 1-paid, 2-shipped, 3-completed, 4-cancelled")
    @Min(value = 0, message = "Order status must be greater than or equal to 0")
    @Max(value = 4, message = "Order status must be less than or equal to 4")
    private Integer status;

    @Schema(description = "Address ID")
    @NotNull(message = "Address ID cannot be null")
    private Long addressId;

    @Schema(description = "Payment time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @Schema(description = "Shipping time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;

    @Schema(description = "Completion time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @Schema(description = "Cancellation time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    @Schema(description = "Cancellation reason")
    @Size(max = 255, message = "Cancellation reason length must be less than or equal to 255")
    private String cancelReason;

    @Schema(description = "Remark")
    @Size(max = 255, message = "Remark length must be less than or equal to 255")
    private String remark;

    @Schema(description = "Creation time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Created by")
    private Long createBy;

    @Schema(description = "Updated by")
    private Long updateBy;

    @Schema(description = "Optimistic lock version")
    private Integer version;

    @Schema(description = "Soft delete flag")
    private Integer deleted;

    @Schema(description = "Order item list")
    private List<OrderItemDTO> orderItems;
}
