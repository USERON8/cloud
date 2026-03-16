package com.cloud.order.service;

import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;

public interface OrderPlacementService {

  OrderAggregateResponse createOrder(CreateMainOrderRequest request);
}
