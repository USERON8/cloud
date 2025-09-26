package com.cloud.payment.controller;

import com.cloud.common.result.Result;
import com.cloud.payment.module.dto.AlipayCreateRequest;
import com.cloud.payment.module.dto.AlipayCreateResponse;
import com.cloud.payment.service.AlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付控制器
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment/alipay")
@RequiredArgsConstructor
@Tag(name = "支付宝支付", description = "支付宝支付相关接口")
public class AlipayController {

    private final AlipayService alipayService;

    @PostMapping("/create")
    @Operation(summary = "创建支付宝支付", description = "创建支付宝支付订单，返回支付表单")
    public Result<AlipayCreateResponse> createPayment(
            @Valid @RequestBody AlipayCreateRequest request) {
        
        log.info("创建支付宝支付请求 - 订单ID: {}, 金额: {}", request.getOrderId(), request.getAmount());
        
        AlipayCreateResponse response = alipayService.createPayment(request);
        
        return Result.success("支付订单创建成功", response);
    }

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "支付宝异步通知", description = "接收支付宝异步通知")
    public String handleNotify(HttpServletRequest request) {
        log.info("收到支付宝异步通知");
        
        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = convertRequestToMap(request);
            
            // 处理通知
            boolean success = alipayService.handleNotify(params);
            
            if (success) {
                log.info("支付宝异步通知处理成功");
                return "success";
            } else {
                log.error("支付宝异步通知处理失败");
                return "failure";
            }
            
        } catch (Exception e) {
            log.error("支付宝异步通知处理异常", e);
            return "failure";
        }
    }

    @GetMapping("/query/{outTradeNo}")
    @Operation(summary = "查询支付状态", description = "查询支付宝支付状态")
    public Result<String> queryPaymentStatus(
            @Parameter(description = "商户订单号") @PathVariable String outTradeNo) {
        
        log.info("查询支付状态 - 订单号: {}", outTradeNo);
        
        String status = alipayService.queryPaymentStatus(outTradeNo);
        
        if (status != null) {
            return Result.success(status, "查询成功");
        } else {
            return Result.error("查询失败");
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "申请退款", description = "申请支付宝退款")
    public Result<Boolean> refund(
            @Parameter(description = "商户订单号") @RequestParam String outTradeNo,
            @Parameter(description = "退款金额") @RequestParam BigDecimal refundAmount,
            @Parameter(description = "退款原因") @RequestParam String refundReason) {
        
        log.info("申请退款 - 订单号: {}, 金额: {}, 原因: {}", outTradeNo, refundAmount, refundReason);
        
        boolean success = alipayService.refund(outTradeNo, refundAmount, refundReason);
        
        if (success) {
            return Result.success("退款申请成功", true);
        } else {
            return Result.error("退款申请失败");
        }
    }

    @PostMapping("/close/{outTradeNo}")
    @Operation(summary = "关闭订单", description = "关闭支付宝订单")
    public Result<Boolean> closeOrder(
            @Parameter(description = "商户订单号") @PathVariable String outTradeNo) {
        
        log.info("关闭订单 - 订单号: {}", outTradeNo);
        
        boolean success = alipayService.closeOrder(outTradeNo);
        
        if (success) {
            return Result.success("订单关闭成功", true);
        } else {
            return Result.error("订单关闭失败");
        }
    }

    @GetMapping("/verify/{outTradeNo}")
    @Operation(summary = "验证支付结果", description = "验证支付宝支付结果")
    public Result<Boolean> verifyPayment(
            @Parameter(description = "商户订单号") @PathVariable String outTradeNo) {
        
        log.info("验证支付结果 - 订单号: {}", outTradeNo);
        
        boolean success = alipayService.verifyPayment(outTradeNo);
        
        return Result.success(success ? "支付成功" : "支付未完成", success);
    }

    /**
     * 将HttpServletRequest转换为Map
     */
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
