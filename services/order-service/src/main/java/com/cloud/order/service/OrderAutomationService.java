package com.cloud.order.service;

public interface OrderAutomationService {

    int autoConfirmShippedOrders();

    int autoApproveTimedOutAfterSales();
}
