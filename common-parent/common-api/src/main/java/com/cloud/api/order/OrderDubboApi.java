package com.cloud.api.order;

import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import java.util.List;

/**
 * 订单服务内部调用契约。
 *
 * <p>主要服务于支付回调、搜索热度和运营统计等跨服务链路。
 */
public interface OrderDubboApi {

  /** 查询子订单状态，供支付、售后等链路确认订单当前状态。 */
  OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo);

  /** 统计今日商品销量排行。 */
  List<ProductSellStatDTO> statSellCountToday(Integer limit);

  /** 按商品 ID 批量统计销量。 */
  List<ProductSellStatDTO> statSellCountByProductIds(List<Long> productIds);
}
