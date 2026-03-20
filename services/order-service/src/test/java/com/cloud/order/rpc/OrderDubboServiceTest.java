package com.cloud.order.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.order.service.OrderQueryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderDubboServiceTest {

  @Mock private OrderQueryService orderQueryService;

  private OrderDubboService orderDubboService;

  @BeforeEach
  void setUp() {
    orderDubboService = new OrderDubboService(orderQueryService);
  }

  @Test
  void getSubOrderStatus_missingMainOrder_returnsNull() {
    when(orderQueryService.getSubOrderStatus("M1", "S1")).thenReturn(null);

    OrderSubStatusVO result = orderDubboService.getSubOrderStatus("M1", "S1");

    assertThat(result).isNull();
  }

  @Test
  void getSubOrderStatus_success_returnsVo() {
    OrderSubStatusVO expected = new OrderSubStatusVO();
    expected.setMainOrderId(10L);
    expected.setSubOrderId(20L);
    expected.setOrderStatus("PAID");
    expected.setUserId(5L);

    when(orderQueryService.getSubOrderStatus("M2", "S2")).thenReturn(expected);

    OrderSubStatusVO result = orderDubboService.getSubOrderStatus("M2", "S2");

    assertThat(result).isNotNull();
    assertThat(result.getMainOrderId()).isEqualTo(10L);
    assertThat(result.getSubOrderId()).isEqualTo(20L);
    assertThat(result.getOrderStatus()).isEqualTo("PAID");
    assertThat(result.getUserId()).isEqualTo(5L);
  }

  @Test
  void statSellCountByProductIds_delegates() {
    ProductSellStatDTO stat = new ProductSellStatDTO();
    stat.setProductId(88L);
    stat.setSellCount(16L);
    when(orderQueryService.statSellCountByProductIds(List.of(88L))).thenReturn(List.of(stat));

    List<ProductSellStatDTO> result = orderDubboService.statSellCountByProductIds(List.of(88L));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSellCount()).isEqualTo(16L);
    verify(orderQueryService).statSellCountByProductIds(List.of(88L));
  }
}
