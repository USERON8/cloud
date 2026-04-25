package com.cloud.payment.controller;

import com.cloud.common.annotation.RawResponse;
import com.cloud.payment.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RawResponse
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "Payment order and refund APIs")
public class PaymentCheckoutController {

  private final PaymentOrderService paymentOrderService;

  @GetMapping(value = "/payment-checkouts/{ticket}", produces = MediaType.TEXT_HTML_VALUE)
  @Operation(summary = "Render payment checkout page")
  public String renderCheckoutPage(@PathVariable String ticket) {
    return paymentOrderService.renderCheckoutPage(ticket);
  }
}
