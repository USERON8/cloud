package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 退款列表查询DTO
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Data
@Schema(description = "退款列表查询参数")
public class RefundPageDTO {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer pageSize = 10;

    @Schema(description = "退款状态：0-待审核，1-审核通过，2-审核拒绝，3-退货中，4-已收货，5-退款中，6-已完成，7-已取消，8-已关闭")
    private Integer status;

    @Schema(description = "用户ID（商家查询时使用）")
    private Long userId;

    @Schema(description = "商家ID（用户查询时自动填充）")
    private Long merchantId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "退款类型：1-仅退款，2-退货退款")
    private Integer refundType;

    @Schema(description = "开始时间（格式：yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束时间（格式：yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "排序字段：created_at(默认), refund_amount")
    private String sortField = "created_at";

    @Schema(description = "排序方式：asc, desc(默认)")
    private String sortOrder = "desc";
}
