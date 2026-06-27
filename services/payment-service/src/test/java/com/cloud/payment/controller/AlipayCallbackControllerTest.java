package com.cloud.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.alipay.api.internal.util.AlipaySignature;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.PaymentDigestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlipayCallbackControllerTest {

  @Mock private PaymentOrderService paymentOrderService;

  private AlipayConfig alipayConfig;
  private AlipayCallbackController controller;

  @BeforeEach
  void setUp() {
    alipayConfig = new AlipayConfig();
    alipayConfig.setAppId("app-1");
    alipayConfig.setMerchantId("merchant-1");
    alipayConfig.setAlipayPublicKey("public-key");
    alipayConfig.setCharset("UTF-8");
    alipayConfig.setSignType("RSA2");
    controller =
        new AlipayCallbackController(
            paymentOrderService, alipayConfig, new ObjectMapper(), new PaymentDigestSupport());
  }

  @Test
  void signedCallbackWithoutAppIdIsRejected() throws Exception {
    Map<String, String> params = validParams();
    params.remove("app_id");

    try (MockedStatic<AlipaySignature> signature = mockSignature(true)) {
      assertThat(controller.handleNotifyCallback(params)).isEqualTo("failure");
      verify(paymentOrderService, never()).handlePaymentCallback(any(), any());
    }
  }

  @Test
  void signedCallbackWithoutSellerIdIsRejected() throws Exception {
    Map<String, String> params = validParams();
    params.remove("seller_id");

    try (MockedStatic<AlipaySignature> signature = mockSignature(true)) {
      assertThat(controller.handleNotifyCallback(params)).isEqualTo("failure");
      verify(paymentOrderService, never()).handlePaymentCallback(any(), any());
    }
  }

  @Test
  void validSignedCallbackInvokesPaymentService() throws Exception {
    Map<String, String> params = validParams();

    try (MockedStatic<AlipaySignature> signature = mockSignature(true)) {
      assertThat(controller.handleNotifyCallback(params)).isEqualTo("success");
      verify(paymentOrderService).handlePaymentCallback(any(), any());
    }
  }

  private MockedStatic<AlipaySignature> mockSignature(boolean result) {
    MockedStatic<AlipaySignature> signature = mockStatic(AlipaySignature.class);
    signature.when(() -> AlipaySignature.rsaCheckV1(any(), any(), any(), any())).thenReturn(result);
    return signature;
  }

  private Map<String, String> validParams() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("app_id", "app-1");
    params.put("seller_id", "merchant-1");
    params.put("out_trade_no", "P202604260001");
    params.put("trade_no", "T202604260001");
    params.put("notify_id", "N202604260001");
    params.put("trade_status", "TRADE_SUCCESS");
    params.put("total_amount", "99.90");
    params.put("sign", "signature");
    return params;
  }
}
