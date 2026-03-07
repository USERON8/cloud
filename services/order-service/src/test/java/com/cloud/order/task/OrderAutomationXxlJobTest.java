package com.cloud.order.task;

import com.cloud.order.service.OrderAutomationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutomationXxlJobTest {

    @Mock
    private OrderAutomationService orderAutomationService;

    @InjectMocks
    private OrderAutomationXxlJob orderAutomationXxlJob;

    @Test
    void autoConfirmReceiptShouldDelegate() {
        when(orderAutomationService.autoConfirmShippedOrders()).thenReturn(4);

        orderAutomationXxlJob.autoConfirmReceipt();

        verify(orderAutomationService).autoConfirmShippedOrders();
    }

    @Test
    void autoApproveAfterSalesShouldDelegate() {
        when(orderAutomationService.autoApproveTimedOutAfterSales()).thenReturn(2);

        orderAutomationXxlJob.autoApproveAfterSales();

        verify(orderAutomationService).autoApproveTimedOutAfterSales();
    }
}
