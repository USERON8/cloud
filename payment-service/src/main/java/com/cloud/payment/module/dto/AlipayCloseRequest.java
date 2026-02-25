package com.cloud.payment.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Alipay close order request")
public class AlipayCloseRequest {

    @NotBlank(message = "Out trade number cannot be blank")
    @Schema(description = "Merchant out trade number", example = "PAY_20260225153000_100001")
    private String outTradeNo;
}
