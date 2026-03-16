package com.cloud.order.rpc;

import com.cloud.api.order.OrderDubboApi;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.order.service.OrderQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = OrderDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class OrderDubboService implements OrderDubboApi {

  private final OrderQueryService orderQueryService;

  @Override
  public OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo) {
    return orderQueryService.getSubOrderStatus(mainOrderNo, subOrderNo);
  }

  @Override
  public List<ProductSellStatDTO> statSellCountToday(Integer limit) {
    return orderQueryService.statSellCountToday(limit);
  }
}
