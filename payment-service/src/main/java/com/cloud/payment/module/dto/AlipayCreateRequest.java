package com.cloud.payment.module.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;







@Data
@Schema(description = "鏀粯瀹濇敮浠樺垱寤鸿姹?)
public class AlipayCreateRequest {

    


    @NotNull(message = "璁㈠崟ID涓嶈兘涓虹┖")
    @Schema(description = "璁㈠崟ID", example = "1234567890")
    private Long orderId;

    


    @NotNull(message = "鏀粯閲戦涓嶈兘涓虹┖")
    @DecimalMin(value = "0.01", message = "鏀粯閲戦蹇呴』澶т簬0.01")
    @Schema(description = "鏀粯閲戦", example = "99.99")
    private BigDecimal amount;

    


    @NotBlank(message = "鍟嗗搧鏍囬涓嶈兘涓虹┖")
    @Schema(description = "鍟嗗搧鏍囬", example = "iPhone 15 Pro Max")
    private String subject;

    


    @Schema(description = "鍟嗗搧鎻忚堪", example = "鑻规灉iPhone 15 Pro Max 256GB 娣辩┖榛戣壊")
    private String body;

    


    @NotNull(message = "鐢ㄦ埛ID涓嶈兘涓虹┖")
    @Schema(description = "鐢ㄦ埛ID", example = "1001")
    private Long userId;

    


    @Schema(description = "鏀粯瓒呮椂鏃堕棿锛堝垎閽燂級", example = "30")
    private Integer timeoutMinutes = 30;

    


    @Schema(description = "鍟嗗搧绫诲瀷", example = "FAST_INSTANT_TRADE_PAY")
    private String productCode = "FAST_INSTANT_TRADE_PAY";
}
