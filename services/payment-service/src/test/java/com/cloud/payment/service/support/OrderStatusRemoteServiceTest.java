package com.cloud.payment.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderStatusRemoteServiceTest {

  @Mock private OrderDubboApi orderDubboApi;

  private OrderStatusRemoteService orderStatusRemoteService;

  @BeforeEach
  void setUp() {
    orderStatusRemoteService = new OrderStatusRemoteService();
    ReflectionTestUtils.setField(orderStatusRemoteService, "orderDubboApi", orderDubboApi);
  }

  @Test
  void getSubOrderStatus_delegates() {
    OrderSubStatusVO vo = new OrderSubStatusVO();
    when(orderDubboApi.getSubOrderStatus("M1", "S1")).thenReturn(vo);

    OrderSubStatusVO result = orderStatusRemoteService.getSubOrderStatus("M1", "S1");

    assertThat(result).isSameAs(vo);
    verify(orderDubboApi).getSubOrderStatus("M1", "S1");
  }
}
