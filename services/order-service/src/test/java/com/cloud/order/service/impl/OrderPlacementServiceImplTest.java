package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReserveTccRemoteService;
import com.cloud.order.tcc.OrderCreateTccAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceImplTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderCreateTccAction orderCreateTccAction;

    @Mock
    private StockReserveTccRemoteService stockReserveTccRemoteService;

    @InjectMocks
    private OrderPlacementServiceImpl orderPlacementService;

    @Test
    void createOrder_success_reservesStock() {
        CreateMainOrderRequest request = new CreateMainOrderRequest();
        request.setIdempotencyKey("abc");
        request.setUserId(10L);
        request.setCartId(99L);

        when(orderCreateTccAction.prepare(eq(null), eq("10:abc"), eq(99L), eq(request)))
                .thenReturn(true);

        OrderMain main = new OrderMain();
        main.setId(100L);
        main.setMainOrderNo("M100");
        when(orderMainMapper.selectActiveByIdempotencyKey("10:abc")).thenReturn(main);

        OrderSub sub = new OrderSub();
        sub.setSubOrderNo("S1");
        sub.setOrderStatus("CREATED");
        OrderItem itemA = new OrderItem();
        itemA.setSkuId(7L);
        itemA.setQuantity(2);
        OrderItem itemB = new OrderItem();
        itemB.setSkuId(7L);
        itemB.setQuantity(1);

        OrderAggregateResponse.SubOrderWithItems wrapped = new OrderAggregateResponse.SubOrderWithItems();
        wrapped.setSubOrder(sub);
        wrapped.setItems(List.of(itemA, itemB));
        OrderAggregateResponse aggregate = new OrderAggregateResponse();
        aggregate.setMainOrder(main);
        aggregate.setSubOrders(List.of(wrapped));
        when(orderService.getOrderAggregate(100L)).thenReturn(aggregate);

        when(stockReserveTccRemoteService.tryReserve(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        OrderAggregateResponse result = orderPlacementService.createOrder(request);

        assertThat(result).isSameAs(aggregate);
        ArgumentCaptor<StockOperateCommandDTO> captor = ArgumentCaptor.forClass(StockOperateCommandDTO.class);
        verify(stockReserveTccRemoteService).tryReserve(captor.capture());
        StockOperateCommandDTO command = captor.getValue();
        assertThat(command.getOrderNo()).isEqualTo("M100");
        assertThat(command.getSubOrderNo()).isEqualTo("S1");
        assertThat(command.getSkuId()).isEqualTo(7L);
        assertThat(command.getQuantity()).isEqualTo(3);
    }

    @Test
    void createOrder_prepareFailed_throws() {
        CreateMainOrderRequest request = new CreateMainOrderRequest();
        request.setIdempotencyKey("abc");
        request.setUserId(10L);
        request.setCartId(99L);

        when(orderCreateTccAction.prepare(eq(null), eq("10:abc"), eq(99L), eq(request)))
                .thenReturn(false);

        assertThatThrownBy(() -> orderPlacementService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("failed to create order aggregate");
    }
}
