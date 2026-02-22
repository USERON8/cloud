package com.cloud.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;







@Data
@Schema(description = "閫€娆句俊鎭?)
public class RefundVO {

    @Schema(description = "閫€娆惧崟ID")
    private Long id;

    @Schema(description = "閫€娆惧崟鍙?)
    private String refundNo;

    @Schema(description = "璁㈠崟ID")
    private Long orderId;

    @Schema(description = "璁㈠崟鍙?)
    private String orderNo;

    @Schema(description = "鐢ㄦ埛ID")
    private Long userId;

    @Schema(description = "鍟嗗ID")
    private Long merchantId;

    @Schema(description = "閫€娆剧被鍨嬶細1-浠呴€€娆撅紝2-閫€璐ч€€娆?)
    private Integer refundType;

    @Schema(description = "閫€娆剧被鍨嬪悕绉?)
    private String refundTypeName;

    @Schema(description = "閫€娆惧師鍥?)
    private String refundReason;

    @Schema(description = "璇︾粏璇存槑")
    private String refundDescription;

    @Schema(description = "閫€娆鹃噾棰?)
    private BigDecimal refundAmount;

    @Schema(description = "閫€璐ф暟閲?)
    private Integer refundQuantity;

    @Schema(description = "閫€娆剧姸鎬侊細0-寰呭鏍革紝1-瀹℃牳閫氳繃锛?-瀹℃牳鎷掔粷锛?-閫€璐т腑锛?-宸叉敹璐э紝5-閫€娆句腑锛?-宸插畬鎴愶紝7-宸插彇娑堬紝8-宸插叧闂?)
    private Integer status;

    @Schema(description = "閫€娆剧姸鎬佸悕绉?)
    private String statusName;

    @Schema(description = "瀹℃牳鏃堕棿")
    private LocalDateTime auditTime;

    @Schema(description = "瀹℃牳澶囨敞")
    private String auditRemark;

    @Schema(description = "鐗╂祦鍏徃")
    private String logisticsCompany;

    @Schema(description = "鐗╂祦鍗曞彿")
    private String logisticsNo;

    @Schema(description = "閫€娆炬椂闂?)
    private LocalDateTime refundTime;

    @Schema(description = "閫€娆炬笭閬?)
    private String refundChannel;

    @Schema(description = "閫€娆句氦鏄撳彿")
    private String refundTransactionNo;

    @Schema(description = "鍒涘缓鏃堕棿")
    private LocalDateTime createdAt;

    @Schema(description = "鏇存柊鏃堕棿")
    private LocalDateTime updatedAt;
}
