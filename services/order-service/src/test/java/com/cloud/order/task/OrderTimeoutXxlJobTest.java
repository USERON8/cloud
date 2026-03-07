package com.cloud.order.task;

import com.cloud.order.service.OrderTimeoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutXxlJobTest {

    @Mock
    private OrderTimeoutService orderTimeoutService;

    @InjectMocks
    private OrderTimeoutXxlJob orderTimeoutXxlJob;

    @Test
    void shouldDelegateTimeoutCheckToService() {
        when(orderTimeoutService.checkAndHandleTimeoutOrders()).thenReturn(3);

        orderTimeoutXxlJob.checkTimeoutOrders();

        verify(orderTimeoutService, times(1)).checkAndHandleTimeoutOrders();
    }
}
