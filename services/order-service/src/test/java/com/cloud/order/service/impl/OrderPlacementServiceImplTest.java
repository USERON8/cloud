package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.assertj.core.groups.Tuple;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private StockReservationRemoteService stockReservationRemoteService;

    @InjectMocks
    private OrderPlacementServiceImpl orderPlacementService;

    @Test
    void createOrderShouldReserveStockAndAdvanceCreatedSubOrders() {
        CreateMainOrderRequest request = new CreateMainOrderRequest();

        OrderMain mainOrder = new OrderMain();
        mainOrder.setId(10L);

        OrderAggregateResponse initialAggregate = aggregate(
                "M100",
                wrappedSubOrder(11L, "S100", "CREATED", item(101L, 1), item(101L, 2), item(102L, 1)),
                wrappedSubOrder(12L, "S200", "STOCK_RESERVED", item(201L, 2))
        );
        OrderAggregateResponse finalAggregate = aggregate(
                "M100",
                wrappedSubOrder(11L, "S100", "STOCK_RESERVED", item(101L, 1), item(101L, 2), item(102L, 1)),
                wrappedSubOrder(12L, "S200", "STOCK_RESERVED", item(201L, 2))
        );

        when(orderService.createMainOrder(request)).thenReturn(mainOrder);
        when(orderService.getOrderAggregate(10L)).thenReturn(initialAggregate, finalAggregate);
        when(stockReservationRemoteService.reserve(any())).thenReturn(true);

        OrderAggregateResponse result = orderPlacementService.createOrder(request);

        assertThat(result).isSameAs(finalAggregate);
        ArgumentCaptor<StockOperateCommandDTO> captor = ArgumentCaptor.forClass(StockOperateCommandDTO.class);
        verify(stockReservationRemoteService, org.mockito.Mockito.times(2)).reserve(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(StockOperateCommandDTO::getSubOrderNo, StockOperateCommandDTO::getSkuId, StockOperateCommandDTO::getQuantity)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("S100", 101L, 3),
                        Tuple.tuple("S100", 102L, 1)
                );
        verify(orderService).advanceSubOrderStatus(11L, "RESERVE");
        verify(orderService, never()).advanceSubOrderStatus(12L, "RESERVE");
    }

    @Test
    void createOrderShouldFailWhenStockReservationFails() {
        CreateMainOrderRequest request = new CreateMainOrderRequest();

        OrderMain mainOrder = new OrderMain();
        mainOrder.setId(20L);

        OrderAggregateResponse aggregate = aggregate(
                "M200",
                wrappedSubOrder(21L, "S201", "CREATED", item(301L, 1))
        );

        when(orderService.createMainOrder(request)).thenReturn(mainOrder);
        when(orderService.getOrderAggregate(20L)).thenReturn(aggregate);
        when(stockReservationRemoteService.reserve(any())).thenReturn(false);

        assertThatThrownBy(() -> orderPlacementService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserve stock failed");
        verify(orderService, never()).advanceSubOrderStatus(any(), any());
    }

    @Test
    void createOrderShouldFailWhenAggregateHasNoSubOrders() {
        CreateMainOrderRequest request = new CreateMainOrderRequest();

        OrderMain mainOrder = new OrderMain();
        mainOrder.setId(30L);

        OrderAggregateResponse aggregate = new OrderAggregateResponse();
        aggregate.setMainOrder(mainOrder);
        aggregate.setSubOrders(List.of());

        when(orderService.createMainOrder(request)).thenReturn(mainOrder);
        when(orderService.getOrderAggregate(30L)).thenReturn(aggregate);

        assertThatThrownBy(() -> orderPlacementService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no sub orders");
        verify(stockReservationRemoteService, never()).reserve(any());
    }

    private OrderAggregateResponse aggregate(String mainOrderNo, OrderAggregateResponse.SubOrderWithItems... subOrders) {
        OrderMain mainOrder = new OrderMain();
        mainOrder.setId(1L);
        mainOrder.setMainOrderNo(mainOrderNo);

        OrderAggregateResponse response = new OrderAggregateResponse();
        response.setMainOrder(mainOrder);
        response.setSubOrders(List.of(subOrders));
        return response;
    }

    private OrderAggregateResponse.SubOrderWithItems wrappedSubOrder(Long subOrderId, String subOrderNo, String status, OrderItem... items) {
        OrderSub subOrder = new OrderSub();
        subOrder.setId(subOrderId);
        subOrder.setSubOrderNo(subOrderNo);
        subOrder.setOrderStatus(status);

        OrderAggregateResponse.SubOrderWithItems wrapped = new OrderAggregateResponse.SubOrderWithItems();
        wrapped.setSubOrder(subOrder);
        wrapped.setItems(List.of(items));
        return wrapped;
    }

    private OrderItem item(Long skuId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        return item;
    }
}
