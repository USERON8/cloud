package com.cloud.order.service;

import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.enums.OrderAction;
import java.util.List;

public interface OrderService {

  OrderMain createMainOrder(CreateMainOrderRequest request);

  OrderMain getMainOrder(Long mainOrderId);

  OrderAggregateResponse getOrderAggregate(Long mainOrderId);

  List<OrderSub> listSubOrders(Long mainOrderId);

  OrderSub getSubOrder(Long subOrderId);

  OrderSub advanceSubOrderStatus(Long subOrderId, OrderAction action);

  AfterSale applyAfterSale(AfterSale afterSale);

  AfterSale getAfterSale(Long afterSaleId);

  AfterSale advanceAfterSaleStatus(Long afterSaleId, AfterSaleAction action, String remark);
}
