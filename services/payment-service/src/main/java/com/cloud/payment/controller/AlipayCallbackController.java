package com.cloud.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.cloud.common.annotation.RawResponse;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.service.PaymentOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RawResponse
@RestController
@RequestMapping("/api/v1/payment/alipay")
@RequiredArgsConstructor
@Tag(name = "Payment Callback API", description = "External payment callback APIs")
public class AlipayCallbackController {

  private final PaymentOrderService paymentOrderService;
  private final AlipayConfig alipayConfig;
  private final ObjectMapper objectMapper;

  @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Operation(summary = "Handle Alipay notify callback")
  public String handleNotifyCallback(@RequestParam Map<String, String> params) {
    try {
      if (!verifySignature(params) || !verifyAppId(params)) {
        return "failure";
      }
      String callbackStatus = resolveCallbackStatus(params.get("trade_status"));
      if (callbackStatus == null) {
        return "success";
      }
      PaymentCallbackCommandDTO command = buildCommand(params, callbackStatus);
      paymentOrderService.handlePaymentCallback(command);
      return "success";
    } catch (Exception ex) {
      log.warn("Handle Alipay notify callback failed", ex);
      return "failure";
    }
  }

  private boolean verifySignature(Map<String, String> params) throws AlipayApiException {
    if (params == null || params.isEmpty()) {
      return false;
    }
    return AlipaySignature.rsaCheckV1(
        params,
        alipayConfig.getAlipayPublicKey(),
        alipayConfig.getCharset(),
        alipayConfig.getSignType());
  }

  private boolean verifyAppId(Map<String, String> params) {
    String appId = params.get("app_id");
    return !StringUtils.hasText(appId) || alipayConfig.getAppId().equals(appId);
  }

  private PaymentCallbackCommandDTO buildCommand(Map<String, String> params, String callbackStatus)
      throws JsonProcessingException {
    PaymentCallbackCommandDTO command = new PaymentCallbackCommandDTO();
    command.setPaymentNo(requireParam(params, "out_trade_no"));
    command.setCallbackNo(resolveCallbackNo(params, callbackStatus));
    command.setCallbackStatus(callbackStatus);
    command.setProviderTxnNo(params.get("trade_no"));
    command.setIdempotencyKey(command.getCallbackNo());
    command.setAmount(parseAmount(params.get("total_amount")));
    command.setPayload(objectMapper.writeValueAsString(new LinkedHashMap<>(params)));
    return command;
  }

  private String requireParam(Map<String, String> params, String key) {
    String value = params.get(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("missing callback field: " + key);
    }
    return value;
  }

  private String resolveCallbackNo(Map<String, String> params, String callbackStatus) {
    String notifyId = params.get("notify_id");
    if (StringUtils.hasText(notifyId)) {
      return notifyId;
    }
    String tradeNo = params.get("trade_no");
    if (StringUtils.hasText(tradeNo)) {
      return tradeNo + ":" + callbackStatus;
    }
    return requireParam(params, "out_trade_no") + ":" + callbackStatus;
  }

  private String resolveCallbackStatus(String tradeStatus) {
    if (!StringUtils.hasText(tradeStatus)) {
      throw new IllegalArgumentException("missing callback field: trade_status");
    }
    return switch (tradeStatus.trim().toUpperCase()) {
      case "TRADE_SUCCESS", "TRADE_FINISHED" -> "SUCCESS";
      case "TRADE_CLOSED" -> "FAIL";
      default -> null;
    };
  }

  private BigDecimal parseAmount(String amount) {
    if (!StringUtils.hasText(amount)) {
      return null;
    }
    return new BigDecimal(amount.trim());
  }
}
