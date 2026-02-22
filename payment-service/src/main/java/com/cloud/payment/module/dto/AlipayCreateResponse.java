package com.cloud.payment.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alipay create payment response")
public class AlipayCreateResponse {

    @Schema(description = "Alipay payment form html")
    private String paymentForm;

    @Schema(description = "Internal payment id")
    private Long paymentId;

    @Schema(description = "Merchant out trade number")
    private String outTradeNo;

    @Schema(description = "Payment status")
    private Integer status;

    @Schema(description = "Response timestamp")
    private Long timestamp;

    @Schema(description = "Request trace id")
    private String traceId;
}
