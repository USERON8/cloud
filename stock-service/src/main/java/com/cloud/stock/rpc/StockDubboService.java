package com.cloud.stock.rpc;

import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.OperationResultVO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = StockFeignClient.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class StockDubboService implements StockFeignClient {

    private final StockService stockService;

    @Override
    public StockVO getStockByProductId(Long productId) {
        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            return null;
        }
        StockVO vo = new StockVO();
        vo.setId(stock.getId());
        vo.setProductId(stock.getProductId());
        vo.setProductName(stock.getProductName());
        vo.setStockQuantity(stock.getStockQuantity());
        vo.setFrozenQuantity(stock.getFrozenQuantity());
        vo.setAvailableQuantity(stock.getAvailableQuantity());
        vo.setStockStatus(stock.getStockStatus());
        vo.setLowStockThreshold(stock.getLowStockThreshold());
        vo.setCreatedAt(stock.getCreatedAt());
        vo.setUpdatedAt(stock.getUpdatedAt());
        return vo;
    }

    @Override
    public OperationResultVO updateStock(Long productId, Integer quantity) {
        StockDTO stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            return OperationResultVO.failure("Stock record not found");
        }
        if (quantity == null || quantity < 0) {
            return OperationResultVO.failure("Quantity must be >= 0");
        }

        Integer current = stock.getStockQuantity() == null ? 0 : stock.getStockQuantity();
        int delta = quantity - current;
        if (delta == 0) {
            return OperationResultVO.success("No stock change");
        }

        boolean success = delta > 0
                ? stockService.stockIn(productId, delta, "Dubbo update stock")
                : stockService.stockOut(productId, -delta, null, null, "Dubbo update stock");

        return success ? OperationResultVO.success("Stock updated") : OperationResultVO.failure("Stock update failed");
    }
}
