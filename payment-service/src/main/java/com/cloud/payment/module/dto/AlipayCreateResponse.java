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
@Schema(description = "鏀粯瀹濇敮浠樺垱寤哄搷搴?)
public class AlipayCreateResponse {

    


    @Schema(description = "鏀粯琛ㄥ崟HTML锛屽墠绔渶瑕佸皢姝TML鎻掑叆椤甸潰骞惰嚜鍔ㄦ彁浜?)
    private String paymentForm;

    


    @Schema(description = "鏀粯ID")
    private Long paymentId;

    


    @Schema(description = "璁㈠崟鍙?)
    private String outTradeNo;

    


    @Schema(description = "鏀粯鐘舵€侊細0-寰呮敮浠橈紝1-鎴愬姛锛?-澶辫触锛?-宸查€€娆?)
    private Integer status;

    


    @Schema(description = "鍒涘缓鏃堕棿鎴?)
    private Long timestamp;

    


    @Schema(description = "璺熻釜ID锛岀敤浜庡箓绛夋€у鐞?)
    private String traceId;
}
