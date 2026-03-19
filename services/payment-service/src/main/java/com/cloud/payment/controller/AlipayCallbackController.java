package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment/alipay")
@RequiredArgsConstructor
@Tag(name = "Payment Callback API", description = "External payment callback APIs")
public class AlipayCallbackController {

  private final PaymentOrderService paymentOrderService;

  @PostMapping("/notify")
  @Operation(summary = "Handle Alipay notify callback")
  public Result<Boolean> handleNotifyCallback(
      @Valid @RequestBody PaymentCallbackCommandDTO command) {
    return Result.success(paymentOrderService.handlePaymentCallback(command));
  }
}
