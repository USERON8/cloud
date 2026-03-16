package com.cloud.api.order;

import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import java.util.List;

public interface OrderDubboApi {

  OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo);

  List<ProductSellStatDTO> statSellCountToday(Integer limit);
}
