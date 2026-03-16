package com.cloud.order.service;

import com.cloud.order.entity.OrderSub;

public interface OrderShippingService {

  OrderSub ship(Long subOrderId, String shippingCompany, String trackingNumber);
}
