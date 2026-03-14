package com.cloud.order.rpc;

import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDubboServiceTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private OrderSubMapper orderSubMapper;

    private OrderDubboService orderDubboService;

    @BeforeEach
    void setUp() {
        orderDubboService = new OrderDubboService(orderMainMapper, orderSubMapper);
    }

    @Test
    void getSubOrderStatus_missingMainOrder_returnsNull() {
        when(orderMainMapper.selectActiveByOrderNo("M1")).thenReturn(null);

        OrderSubStatusVO result = orderDubboService.getSubOrderStatus("M1", "S1");

        assertThat(result).isNull();
    }

    @Test
    void getSubOrderStatus_success_returnsVo() {
        OrderMain main = new OrderMain();
        main.setId(10L);
        main.setMainOrderNo("M2");
        main.setUserId(5L);
        OrderSub sub = new OrderSub();
        sub.setId(20L);
        sub.setSubOrderNo("S2");
        sub.setOrderStatus("PAID");

        when(orderMainMapper.selectActiveByOrderNo("M2")).thenReturn(main);
        when(orderSubMapper.selectActiveByMainOrderIdAndSubOrderNo(10L, "S2")).thenReturn(sub);

        OrderSubStatusVO result = orderDubboService.getSubOrderStatus("M2", "S2");

        assertThat(result).isNotNull();
        assertThat(result.getMainOrderId()).isEqualTo(10L);
        assertThat(result.getSubOrderId()).isEqualTo(20L);
        assertThat(result.getOrderStatus()).isEqualTo("PAID");
        assertThat(result.getUserId()).isEqualTo(5L);
    }
}
