package com.cloud.order.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentOrderRemoteServiceTest {

  @Mock private PaymentDubboApi paymentDubboApi;

  private PaymentOrderRemoteService paymentOrderRemoteService;

  @BeforeEach
  void setUp() {
    paymentOrderRemoteService = new PaymentOrderRemoteService();
    ReflectionTestUtils.setField(paymentOrderRemoteService, "paymentDubboApi", paymentDubboApi);
  }

  @Test
  void getPaymentOrderByOrderNo_delegates() {
    PaymentOrderVO vo = new PaymentOrderVO();
    when(paymentDubboApi.getPaymentOrderByOrderNo("M1", "S1")).thenReturn(vo);

    PaymentOrderVO result = paymentOrderRemoteService.getPaymentOrderByOrderNo("M1", "S1");

    assertThat(result).isSameAs(vo);
    verify(paymentDubboApi).getPaymentOrderByOrderNo("M1", "S1");
  }
}
