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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment/alipay")
@RequiredArgsConstructor
@Tag(name = "Alipay API", description = "Alipay payment operations")
public class AlipayController {

    private final AlipayService alipayService;

    @PostMapping("/create")
    @Operation(summary = "Create Alipay payment", description = "Create a payment order and return payment form")
    public Result<AlipayCreateResponse> createPayment(@Valid @RequestBody AlipayCreateRequest request) {
        AlipayCreateResponse response = alipayService.createPayment(request);
        return Result.success("Create payment success", response);
    }

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Handle Alipay notify", description = "Handle asynchronous Alipay callback")
    public String handleNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = convertRequestToMap(request);
            boolean success = alipayService.handleNotify(params);
            return success ? "success" : "failure";
        } catch (Exception e) {
            log.error("Handle Alipay notify failed", e);
            return "failure";
        }
    }

    @GetMapping("/query/{outTradeNo}")
    @Operation(summary = "Query payment status", description = "Query Alipay payment status")
    public Result<String> queryPaymentStatus(
            @Parameter(description = "Merchant out trade number") @PathVariable String outTradeNo) {
        String status = alipayService.queryPaymentStatus(outTradeNo);
        if (status == null) {
            return Result.error("Query payment status failed");
        }
        return Result.success("Query payment status success", status);
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment", description = "Create Alipay refund request")
    public Result<Boolean> refund(
            @Parameter(description = "Merchant out trade number") @RequestParam String outTradeNo,
            @Parameter(description = "Refund amount") @RequestParam BigDecimal refundAmount,
            @Parameter(description = "Refund reason") @RequestParam String refundReason) {
        boolean success = alipayService.refund(outTradeNo, refundAmount, refundReason);
        if (!success) {
            return Result.error("Refund failed");
        }
        return Result.success("Refund success", true);
    }

    @PostMapping("/close/{outTradeNo}")
    @Operation(summary = "Close payment order", description = "Close Alipay order")
    public Result<Boolean> closeOrder(
            @Parameter(description = "Merchant out trade number") @PathVariable String outTradeNo) {
        boolean success = alipayService.closeOrder(outTradeNo);
        if (!success) {
            return Result.error("Close order failed");
        }
        return Result.success("Close order success", true);
    }

    @GetMapping("/verify/{outTradeNo}")
    @Operation(summary = "Verify payment", description = "Verify whether payment is completed")
    public Result<Boolean> verifyPayment(
            @Parameter(description = "Merchant out trade number") @PathVariable String outTradeNo) {
        boolean success = alipayService.verifyPayment(outTradeNo);
        return Result.success(success ? "Payment verified" : "Payment is not successful", success);
    }

    private Map<String, String> convertRequestToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                continue;
            }
            params.put(entry.getKey(), String.join(",", values));
        }
        return params;
    }
}
