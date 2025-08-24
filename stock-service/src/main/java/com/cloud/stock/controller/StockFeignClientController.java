package com.cloud.stock.controller;

import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存服务Feign客户端接口实现控制器
 * 实现库存服务对外提供的Feign接口
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StockFeignClientController implements StockFeignClient {

    private final StockService stockService;
    private final StockConverter stockConverter = StockConverter.INSTANCE;

    /**
     * 根据商品ID查询库存
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    public Result<StockVO> getStockByProductId(Long productId) {
        log.info("Feign调用：根据商品ID查询库存，商品ID: {}", productId);
        try {
            Stock stock = stockService.getByProductId(productId);
            if (stock != null) {
                return Result.success(stockConverter.toVO(stock));
            } else {
                return Result.error("未找到该商品的库存信息");
            }
        } catch (Exception e) {
            log.error("Feign调用：查询商品库存失败，商品ID: {}", productId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新库存数量
     *
     * @param productId 商品ID
     * @param quantity  库存数量
     * @param currentUserId 当前用户ID
     * @return 操作结果
     */
    public Result<String> updateStock(Long productId, Integer quantity,
                                      @RequestHeader("X-User-ID") String currentUserId) {
        log.info("Feign调用：更新库存数量，商品ID: {}，数量: {}，操作人: {}", productId, quantity, currentUserId);
        try {
            Stock stock = stockService.getByProductId(productId);
            if (stock == null) {
                return Result.error("未找到该商品的库存信息");
            }

            stock.setStockQuantity(quantity);
            boolean updated = stockService.updateById(stock);

            return updated ? Result.success("库存更新成功") : Result.error("库存更新失败");
        } catch (Exception e) {
            log.error("Feign调用：更新库存数量失败，商品ID: {}，数量: {}", productId, quantity, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}