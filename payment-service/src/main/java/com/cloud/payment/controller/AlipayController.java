package com.cloud.payment.controller;

import com.cloud.common.result.Result;
import com.cloud.payment.module.dto.AlipayCreateRequest;
import com.cloud.payment.module.dto.AlipayCreateResponse;
import com.cloud.payment.service.AlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;







@Slf4j
@RestController
@RequestMapping("/api/v1/payment/alipay")
@RequiredArgsConstructor
@Tag(name = "鏀粯瀹濇敮浠?, description = "鏀粯瀹濇敮浠樼浉鍏虫帴鍙?)
public class AlipayController {

    private final AlipayService alipayService;

    @PostMapping("/create")
    @Operation(summary = "鍒涘缓鏀粯瀹濇敮浠?, description = "鍒涘缓鏀粯瀹濇敮浠樿鍗曪紝杩斿洖鏀粯琛ㄥ崟")
    public Result<AlipayCreateResponse> createPayment(
            @Valid @RequestBody AlipayCreateRequest request) {

        

        AlipayCreateResponse response = alipayService.createPayment(request);

        return Result.success("鏀粯璁㈠崟鍒涘缓鎴愬姛", response);
    }

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "鏀粯瀹濆紓姝ラ€氱煡", description = "鎺ユ敹鏀粯瀹濆紓姝ラ€氱煡")
    public String handleNotify(HttpServletRequest request) {
        

        try {
            
            Map<String, String> params = convertRequestToMap(request);

            
            boolean success = alipayService.handleNotify(params);

            if (success) {
                
                return "success";
            } else {
                log.error("鏀粯瀹濆紓姝ラ€氱煡澶勭悊澶辫触");
                return "failure";
            }

        } catch (Exception e) {
            log.error("鏀粯瀹濆紓姝ラ€氱煡澶勭悊寮傚父", e);
            return "failure";
        }
    }

    @GetMapping("/query/{outTradeNo}")
    @Operation(summary = "鏌ヨ鏀粯鐘舵€?, description = "鏌ヨ鏀粯瀹濇敮浠樼姸鎬?)
    public Result<String> queryPaymentStatus(
            @Parameter(description = "鍟嗘埛璁㈠崟鍙?) @PathVariable String outTradeNo) {

        

        String status = alipayService.queryPaymentStatus(outTradeNo);

        if (status != null) {
            return Result.success(status, "鏌ヨ鎴愬姛");
        } else {
            return Result.error("鏌ヨ澶辫触");
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "鐢宠閫€娆?, description = "鐢宠鏀粯瀹濋€€娆?)
    public Result<Boolean> refund(
            @Parameter(description = "鍟嗘埛璁㈠崟鍙?) @RequestParam String outTradeNo,
            @Parameter(description = "閫€娆鹃噾棰?) @RequestParam BigDecimal refundAmount,
            @Parameter(description = "閫€娆惧師鍥?) @RequestParam String refundReason) {

        

        boolean success = alipayService.refund(outTradeNo, refundAmount, refundReason);

        if (success) {
            return Result.success("閫€娆剧敵璇锋垚鍔?, true);
        } else {
            return Result.error("閫€娆剧敵璇峰け璐?);
        }
    }

    @PostMapping("/close/{outTradeNo}")
    @Operation(summary = "鍏抽棴璁㈠崟", description = "鍏抽棴鏀粯瀹濊鍗?)
    public Result<Boolean> closeOrder(
            @Parameter(description = "鍟嗘埛璁㈠崟鍙?) @PathVariable String outTradeNo) {

        

        boolean success = alipayService.closeOrder(outTradeNo);

        if (success) {
            return Result.success("璁㈠崟鍏抽棴鎴愬姛", true);
        } else {
            return Result.error("璁㈠崟鍏抽棴澶辫触");
        }
    }

    @GetMapping("/verify/{outTradeNo}")
    @Operation(summary = "楠岃瘉鏀粯缁撴灉", description = "楠岃瘉鏀粯瀹濇敮浠樼粨鏋?)
    public Result<Boolean> verifyPayment(
            @Parameter(description = "鍟嗘埛璁㈠崟鍙?) @PathVariable String outTradeNo) {

        

        boolean success = alipayService.verifyPayment(outTradeNo);

        return Result.success(success ? "鏀粯鎴愬姛" : "鏀粯鏈畬鎴?, success);
    }

    


    private Map<String, String> convertRequestToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        return params;
    }
}
