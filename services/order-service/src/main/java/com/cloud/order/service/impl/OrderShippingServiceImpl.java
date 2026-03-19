package com.cloud.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import com.cloud.order.service.support.OrderAggregateCacheService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderShippingServiceImpl implements OrderShippingService {

  private final OrderSubMapper orderSubMapper;
  private final OrderService orderService;
  private final OrderAggregateCacheService orderAggregateCacheService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrderSub ship(Long subOrderId, String shippingCompany, String trackingNumber) {
    if (subOrderId == null) {
      throw new BizException("sub order id is required");
    }
    if (StrUtil.isBlank(shippingCompany) || StrUtil.isBlank(trackingNumber)) {
      throw new BizException("shipping company and tracking number are required");
    }
    OrderSub subOrder = orderSubMapper.selectById(subOrderId);
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      throw new BizException("sub order not found");
    }
    if (!"PAID".equals(subOrder.getOrderStatus()) && !"SHIPPED".equals(subOrder.getOrderStatus())) {
      throw new BizException("order is not ready to ship: " + subOrder.getOrderStatus());
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDate estimatedArrival = LocalDate.now().plusDays(3);
    int updated =
        orderSubMapper.updateShippingInfo(
            subOrderId,
            shippingCompany.trim(),
            trackingNumber.trim(),
            estimatedArrival,
            now,
            "SHIPPED");
    if (updated == 0) {
      throw new BizException("failed to update shipping info");
    }

    if (!"SHIPPED".equals(subOrder.getOrderStatus())) {
      orderService.advanceSubOrderStatus(subOrderId, OrderAction.SHIP);
    }

    OrderSub latest = orderSubMapper.selectById(subOrderId);
    orderAggregateCacheService.evict(latest.getMainOrderId());
    return latest;
  }
}
