package com.cloud.payment.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlipayPaymentProviderGatewayTest {

  @Mock private AlipayClient alipayClient;

  @Test
  void buildCheckoutPageShouldAppendPaymentContextToReturnUrl() throws Exception {
    AlipayConfig alipayConfig = new AlipayConfig();
    alipayConfig.setReturnUrl("https://shop.example.com/#/pages/app/payments/index?source=alipay");
    AlipayTradeWapPayResponse response = new AlipayTradeWapPayResponse();
    response.setBody("<html>checkout</html>");
    when(alipayClient.pageExecute(any(AlipayTradeWapPayRequest.class), eq("GET")))
        .thenReturn(response);

    AlipayPaymentProviderGateway gateway =
        new AlipayPaymentProviderGateway(alipayClient, alipayConfig, new ObjectMapper());

    String html = gateway.buildCheckoutPage(buildOrder("PAY-1001"));

    ArgumentCaptor<AlipayTradeWapPayRequest> requestCaptor =
        ArgumentCaptor.forClass(AlipayTradeWapPayRequest.class);
    verify(alipayClient).pageExecute(requestCaptor.capture(), eq("GET"));
    assertThat(html).isEqualTo("<html>checkout</html>");
    assertThat(requestCaptor.getValue().getReturnUrl())
        .isEqualTo(
            "https://shop.example.com/#/pages/app/payments/index?source=alipay&paymentNo=PAY-1001&autoPoll=1");
  }

  private PaymentOrderEntity buildOrder(String paymentNo) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setPaymentNo(paymentNo);
    order.setMainOrderNo("MAIN-1");
    order.setSubOrderNo("SUB-1");
    order.setAmount(BigDecimal.valueOf(99.9));
    order.setChannel("ALIPAY");
    return order;
  }
}
