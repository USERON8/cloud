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
@Schema(description = "Alipay trade status response")
public class AlipayTradeStatusResponse {

    @Schema(description = "Whether query call is successful")
    private boolean success;

    @Schema(description = "Merchant out trade number")
    private String outTradeNo;

    @Schema(description = "Alipay trade number")
    private String tradeNo;

    @Schema(description = "Raw trade status")
    private String tradeStatus;

    @Schema(description = "Readable trade status")
    private String tradeStatusDescription;
}
