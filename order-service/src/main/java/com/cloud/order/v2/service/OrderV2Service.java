package com.cloud.order.v2.service;

import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;

import java.util.List;

public interface OrderV2Service {

    OrderMainV2 createMainOrder(CreateMainOrderRequest request);

    List<OrderSubV2> listSubOrders(Long mainOrderId);

    OrderSubV2 advanceSubOrderStatus(Long subOrderId, String action);

    AfterSaleV2 applyAfterSale(AfterSaleV2 afterSale);

    AfterSaleV2 advanceAfterSaleStatus(Long afterSaleId, String action, String remark);
}

