package com.cloud.order.service;

import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.result.PageResult;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.OrderMain;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface OrderQueryService {

  PageResult<OrderSummaryDTO> listOrders(
      Authentication authentication,
      Integer page,
      Integer size,
      Long userId,
      Long shopId,
      Integer status);

  OrderSummaryDTO getOrderSummary(Long orderId, Authentication authentication);

  OrderMain requireAccessibleMainOrder(Long orderId, Authentication authentication);

  void updateCancelReason(Long mainOrderId, String cancelReason);

  OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo);

  List<ProductSellStatDTO> statSellCountToday(Integer limit);
}
