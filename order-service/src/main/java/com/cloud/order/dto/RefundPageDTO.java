package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Refund page query")
public class RefundPageDTO {

    @Schema(description = "Page number", example = "1")
    @Min(value = 1, message = "pageNum must be at least 1")
    private Integer pageNum = 1;

    @Schema(description = "Page size", example = "10")
    @Min(value = 1, message = "pageSize must be at least 1")
    private Integer pageSize = 10;

    @Schema(description = "Refund status")
    private Integer status;

    @Schema(description = "User ID filter")
    private Long userId;

    @Schema(description = "Merchant ID filter")
    private Long merchantId;

    @Schema(description = "Order number")
    private String orderNo;

    @Schema(description = "Refund number")
    private String refundNo;

    @Schema(description = "Refund type: 1 refund only, 2 return and refund")
    private Integer refundType;

    @Schema(description = "Start date, format yyyy-MM-dd")
    private String startDate;

    @Schema(description = "End date, format yyyy-MM-dd")
    private String endDate;

    @Schema(description = "Sort field, default created_at")
    private String sortField = "created_at";

    @Schema(description = "Sort order: asc or desc, default desc")
    private String sortOrder = "desc";
}