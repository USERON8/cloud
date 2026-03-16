package com.cloud.payment.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.payment.service.PaymentOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOrderDubboServiceTest {

  @Mock private PaymentOrderService paymentOrderService;

  private PaymentOrderDubboService paymentOrderDubboService;

  @BeforeEach
  void setUp() {
    paymentOrderDubboService = new PaymentOrderDubboService(paymentOrderService);
  }

  @Test
  void createPaymentOrder_delegates() {
    PaymentOrderCommandDTO command = new PaymentOrderCommandDTO();
    when(paymentOrderService.createPaymentOrder(command)).thenReturn(10L);

    Long result = paymentOrderDubboService.createPaymentOrder(command);

    assertThat(result).isEqualTo(10L);
    verify(paymentOrderService).createPaymentOrder(command);
  }

  @Test
  void getPaymentOrderByNo_delegates() {
    PaymentOrderVO vo = new PaymentOrderVO();
    when(paymentOrderService.getPaymentOrderByNo("P1")).thenReturn(vo);

    PaymentOrderVO result = paymentOrderDubboService.getPaymentOrderByNo("P1");

    assertThat(result).isSameAs(vo);
  }
}
