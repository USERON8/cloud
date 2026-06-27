package com.cloud.api.stock;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import java.util.List;

/**
 * 库存服务内部调用契约。
 *
 * <p>订单创建、支付确认和取消释放库存都通过该接口完成，库存服务负责保证库存流水和可用量一致。
 */
public interface StockDubboApi {

  /** 按 SKU ID 查询库存台账。 */
  StockLedgerVO getLedgerBySkuId(Long skuId);

  /** 按 SKU ID 批量查询库存台账。 */
  List<StockLedgerVO> listLedgersBySkuIds(List<Long> skuIds);

  /** 预检查库存是否满足本次操作要求。 */
  Boolean preCheck(List<StockOperateCommandDTO> commands);

  /** 预占库存，通常在订单创建链路中调用。 */
  Boolean reserve(StockOperateCommandDTO command);

  /** 确认扣减预占库存，通常在支付成功链路中调用。 */
  Boolean confirm(StockOperateCommandDTO command);

  /** 释放预占库存，通常在订单取消或支付超时链路中调用。 */
  Boolean release(StockOperateCommandDTO command);

  /** 回滚库存操作，用于异常补偿。 */
  Boolean rollback(StockOperateCommandDTO command);
}
